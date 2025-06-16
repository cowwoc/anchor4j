package io.github.cowwoc.anchor4j.core.internal.resource;

import io.github.cowwoc.anchor4j.core.exception.ContextNotFoundException;
import io.github.cowwoc.anchor4j.core.exception.UnsupportedExporterException;
import io.github.cowwoc.anchor4j.core.internal.client.InternalClient;
import io.github.cowwoc.anchor4j.core.internal.client.Processes;
import io.github.cowwoc.anchor4j.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.BuildListener;
import io.github.cowwoc.anchor4j.core.resource.BuildListener.Output;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.core.resource.CoreImage;
import io.github.cowwoc.anchor4j.core.resource.CoreImageBuilder;
import io.github.cowwoc.anchor4j.core.resource.DefaultBuildListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * The default implementation of {@code CoreImageBuilder}.
 */
public class DefaultCoreImageBuilder implements CoreImageBuilder
{
	private final InternalClient client;
	private Path dockerfile;
	private final Set<String> platforms = new HashSet<>();
	private final Set<String> tags = new HashSet<>();
	private final Set<String> cacheFrom = new HashSet<>();
	private final Set<Exporter> exporters = new LinkedHashSet<>();
	private String builder = "";
	private BuildListener listener = new DefaultBuildListener();
	private final Logger log = LoggerFactory.getLogger(DefaultCoreImageBuilder.class);

	/**
	 * Creates an image builder.
	 *
	 * @param client the client configuration
	 */
	public DefaultCoreImageBuilder(InternalClient client)
	{
		assert client != null;
		this.client = client;
	}

	@Override
	public CoreImageBuilder dockerfile(Path dockerfile)
	{
		requireThat(dockerfile, "dockerfile").isNotNull();
		this.dockerfile = dockerfile;
		return this;
	}

	@Override
	public CoreImageBuilder platform(String platform)
	{
		requireThat(platform, "platform").doesNotContainWhitespace().isNotEmpty();
		this.platforms.add(platform);
		return this;
	}

	@Override
	public CoreImageBuilder reference(String reference)
	{
		ParameterValidator.validateImageReference(reference, "reference");
		this.tags.add(reference);
		return this;
	}

	@Override
	public CoreImageBuilder cacheFrom(String source)
	{
		requireThat(source, "source").doesNotContainWhitespace().isNotEmpty();
		this.cacheFrom.add(source);
		return this;
	}

	@Override
	public CoreImageBuilder export(Exporter exporter)
	{
		requireThat(exporter, "exporter").isNotNull();
		this.exporters.add(exporter);
		return this;
	}

	@Override
	public CoreImageBuilder builder(String builder)
	{
		ParameterValidator.validateName(builder, "builder");
		this.builder = builder;
		return this;
	}

	@Override
	public CoreImageBuilder listener(BuildListener listener)
	{
		requireThat(listener, "listener").isNotNull();
		this.listener = listener;
		return this;
	}

	@Override
	public CoreImage build(String buildContext) throws IOException, InterruptedException
	{
		return build(Path.of(buildContext));
	}

	@Override
	public CoreImage build(Path buildContext) throws IOException, InterruptedException
	{
		// Path.relativize() requires both Paths to be relative or absolute
		Path absoluteBuildContext = buildContext.toAbsolutePath().normalize();

		// https://docs.docker.com/reference/cli/docker/buildx/build/
		List<String> arguments = new ArrayList<>(2 + cacheFrom.size() + 3 + exporters.size() * 2 + 1 +
			tags.size() * 2 + 2 + 1);
		arguments.add("buildx");
		arguments.add("build");
		if (!cacheFrom.isEmpty())
		{
			for (String source : cacheFrom)
				arguments.add("--cache-from=" + source);
		}
		if (dockerfile != null)
		{
			arguments.add("--file");
			arguments.add(dockerfile.toAbsolutePath().toString());
		}
		if (!platforms.isEmpty())
			arguments.add("--platform=" + String.join(",", platforms));

		boolean outputsImage = false;
		for (Exporter exporter : exporters)
		{
			arguments.add("--output");
			arguments.add(exporter.toCommandLine());
			outputsImage = exporter.outputsImage();
		}
		for (String tag : tags)
		{
			arguments.add("--tag");
			arguments.add(tag);
		}
		if (!builder.isEmpty())
		{
			arguments.add("--builder");
			arguments.add(builder);
		}

		if (outputsImage)
		{
			// Write the imageId to a file and return it to the user
			Path imageIdFile = Files.createTempFile(null, null);
			try
			{
				arguments.add("--iidfile");
				arguments.add(imageIdFile.toString());
				build2(arguments, absoluteBuildContext);
				return client.image(Files.readString(imageIdFile));
			}
			finally
			{
				// If the build fails, docker deletes the imageIdFile on exit so we shouldn't assume that the file
				// still exists.
				Files.deleteIfExists(imageIdFile);
			}
		}
		build2(arguments, absoluteBuildContext);
		return null;
	}

	/**
	 * Common code at the end of the build step.
	 *
	 * @param arguments            the command-line arguments
	 * @param absoluteBuildContext the absolute path of the build context
	 * @throws IOException                  if an I/O error occurs. These errors are typically transient, and
	 *                                      retrying the request may resolve the issue.
	 * @throws InterruptedException         if the thread is interrupted before the operation completes. This
	 *                                      can happen due to shutdown signals.
	 * @throws ContextNotFoundException     if the Docker context cannot be found or resolved
	 * @throws UnsupportedExporterException if the builder driver does not support one of the requested
	 *                                      exporters
	 */
	private void build2(List<String> arguments, Path absoluteBuildContext)
		throws IOException, InterruptedException
	{
		arguments.add(absoluteBuildContext.toString());
		try
		{
			client.retry(_ ->
			{
				ProcessBuilder processBuilder = client.getProcessBuilder(arguments);
				log.debug("Running: {}", processBuilder.command());
				Process process = processBuilder.start();
				listener.buildStarted(process.inputReader(), process.errorReader(), process::waitFor);
				Output output = listener.waitUntilBuildCompletes();

				int exitCode = output.exitCode();
				if (exitCode == 0)
					listener.buildPassed();
				else
				{
					List<String> command = List.copyOf(processBuilder.command());
					Path workingDirectory = Processes.getWorkingDirectory(processBuilder);
					CommandResult result = new CommandResult(command, workingDirectory, output.stdout(),
						output.stderr(), exitCode);
					listener.buildFailed(result);
					client.commandFailed(result);
					throw result.unexpectedResponse();
				}
				return null;
			});
		}
		finally
		{
			listener.buildCompleted();
		}
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultCoreImageBuilder.class).
			add("platforms", platforms).
			add("tags", tags).
			toString();
	}
}