package io.github.cowwoc.anchor4j.container.core.internal.resource;

import io.github.cowwoc.anchor4j.container.core.internal.client.InternalContainerClient;
import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.container.core.resource.BuildListener;
import io.github.cowwoc.anchor4j.container.core.resource.BuildListener.Output;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImageBuilder;
import io.github.cowwoc.anchor4j.container.core.resource.DefaultBuildListener;
import io.github.cowwoc.anchor4j.core.internal.util.Processes;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public class DefaultContainerImageBuilder implements ContainerImageBuilder
{
	private final InternalContainerClient client;
	private Path dockerfile;
	private final Set<String> platforms = new HashSet<>();
	private final Set<String> tags = new HashSet<>();
	private final Set<String> cacheFrom = new HashSet<>();
	private final Set<AbstractExporter> exporters = new LinkedHashSet<>();
	private boolean loadsIntoImageStore;
	private String builder = "";
	private BuildListener listener = new DefaultBuildListener();
	private final Logger log = LoggerFactory.getLogger(DefaultContainerImageBuilder.class);

	/**
	 * Creates an image builder.
	 *
	 * @param client the client configuration
	 */
	public DefaultContainerImageBuilder(InternalContainerClient client)
	{
		assert client != null;
		this.client = client;
	}

	@Override
	public ContainerImageBuilder dockerfile(Path dockerfile)
	{
		requireThat(dockerfile, "dockerfile").isNotNull();
		this.dockerfile = dockerfile;
		return this;
	}

	@Override
	public ContainerImageBuilder platform(String platform)
	{
		requireThat(platform, "platform").doesNotContainWhitespace().isNotEmpty();
		this.platforms.add(platform);
		return this;
	}

	@Override
	public ContainerImageBuilder reference(String reference)
	{
		ParameterValidator.validateImageReference(reference, "reference");
		this.tags.add(reference);
		return this;
	}

	@Override
	public ContainerImageBuilder cacheFrom(String source)
	{
		requireThat(source, "source").doesNotContainWhitespace().isNotEmpty();
		this.cacheFrom.add(source);
		return this;
	}

	@Override
	public ContainerImageBuilder export(Exporter exporter)
	{
		requireThat(exporter, "exporter").isNotNull();
		AbstractExporter ae = (AbstractExporter) exporter;
		this.exporters.add(ae);
		loadsIntoImageStore |= ae.loadsIntoImageStore();
		return this;
	}

	@Override
	public ContainerImageBuilder builder(String builder)
	{
		ParameterValidator.validateName(builder, "builder");
		this.builder = builder;
		return this;
	}

	@Override
	public ContainerImageBuilder listener(BuildListener listener)
	{
		requireThat(listener, "listener").isNotNull();
		this.listener = listener;
		return this;
	}

	@Override
	public ContainerImage apply(String buildContext) throws IOException, InterruptedException
	{
		return apply(Path.of(buildContext));
	}

	@Override
	public ContainerImage apply(Path buildContext) throws IOException, InterruptedException
	{
		List<String> arguments = getArguments(buildContext);
		try
		{
			return client.retry(_ ->
			{
				ProcessBuilder processBuilder = client.getProcessBuilder(arguments);
				log.debug("Running: {}", processBuilder.command());
				Process process = processBuilder.start();
				listener.buildStarted(process.inputReader(), process.errorReader(), process::waitFor);
				Output output = listener.waitUntilBuildCompletes();

				int exitCode = output.exitCode();
				if (exitCode != 0)
				{
					CommandResult result = getCommandResult(processBuilder, output);
					listener.buildFailed(result);
					client.commandFailed(result);
					throw result.unexpectedResponse();
				}
				listener.buildPassed();

				if (loadsIntoImageStore)
				{
					CommandResult result = getCommandResult(processBuilder, output);
					ContainerImage.Id id = client.getBuildXParser().getImageIdFromBuildOutput(result);
					ContainerImage image = client.getImage(id);
					assert image != null;
					return image;
				}
				return null;
			});
		}
		finally
		{
			listener.buildCompleted();
		}
	}

	/**
	 * @param processBuilder the ProcessBuilder used to run the build
	 * @param output         the build output
	 * @return the CommandResult for the build
	 */
	private CommandResult getCommandResult(ProcessBuilder processBuilder, Output output)
	{
		List<String> command = List.copyOf(processBuilder.command());
		Path workingDirectory = Processes.getWorkingDirectory(processBuilder);
		return new CommandResult(command, workingDirectory, output.stdout(),
			output.stderr(), output.exitCode());
	}

	/**
	 * Returns the build's command-line arguments.
	 *
	 * @param buildContext the build context, the directory relative to which paths in the Dockerfile are
	 *                     evaluated
	 * @return the command-line arguments
	 * @throws NullPointerException if {@code buildContext} is null
	 */
	private List<String> getArguments(Path buildContext)
	{
		// Path.relativize() requires both Paths to be relative or absolute
		Path absoluteBuildContext = buildContext.toAbsolutePath().normalize();

		// https://docs.docker.com/reference/cli/docker/buildx/build/
		List<String> arguments = new ArrayList<>(2 + cacheFrom.size() + 2 + 1 + exporters.size() * 2 +
			tags.size() * 2 + 2 + 2);
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

		for (AbstractExporter exporter : exporters)
		{
			arguments.add("--output");
			arguments.add(exporter.toCommandLine());
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
		arguments.add(absoluteBuildContext.toString());
		return arguments;
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultContainerImageBuilder.class).
			add("platforms", platforms).
			add("tags", tags).
			toString();
	}

	public abstract static sealed class AbstractExporter implements Exporter
	{
		/**
		 * Returns the type of the exporter.
		 *
		 * @return the type
		 */
		protected abstract String getType();

		/**
		 * Indicates whether the exporter automatically loads the generated image into an image store, such as the
		 * Docker Engine or a remote image registry.
		 *
		 * @return {@code true} if the image will be loaded into an image store
		 */
		protected abstract boolean loadsIntoImageStore();

		/**
		 * Returns the command-line representation of this option.
		 *
		 * @return the command-line value
		 */
		protected abstract String toCommandLine();

		@Override
		public int hashCode()
		{
			return toCommandLine().hashCode();
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof AbstractExporter other && other.toCommandLine().equals(toCommandLine());
		}
	}

	/**
	 * Builds an exporter that outputs the contents of images to disk.
	 */
	public static final class DefaultContentsExporterBuilder extends AbstractImageExporterBuilder
		implements ContentsExporterBuilder
	{
		private final String path;
		private boolean directory;

		/**
		 * Creates a DefaultContentsExporterBuilder.
		 *
		 * @param path the output location, which is either a TAR archive or a directory depending on whether
		 *             {@link #directory()} is invoked
		 * @throws NullPointerException     if {@code path} is null
		 * @throws IllegalArgumentException if {@code path} contains whitespace or is empty
		 */
		public DefaultContentsExporterBuilder(String path)
		{
			requireThat(path, "path").doesNotContainWhitespace().isNotEmpty();
			this.path = path;
		}

		@Override
		public ContainerImageBuilder.ContentsExporterBuilder directory()
		{
			this.directory = true;
			return this;
		}

		@Override
		public Exporter build()
		{
			return new ExporterAdapter();
		}

		public final class ExporterAdapter extends AbstractExporter
		{
			@Override
			public String getType()
			{
				if (directory)
					return "local";
				return "tar";
			}

			@Override
			public boolean loadsIntoImageStore()
			{
				return false;
			}

			@Override
			public String toCommandLine()
			{
				if (directory)
					return "type=local,dest=" + path;
				return "type=tar,dest=" + path;
			}
		}
	}

	/**
	 * Builds an exporter that outputs images.
	 */
	public abstract static class AbstractImageExporterBuilder
		implements ImageExporterBuilder
	{
		/**
		 * The name of the image.
		 */
		protected String name = "";
		/**
		 * The type of compression to use.
		 */
		protected CompressionType compressionType = CompressionType.GZIP;
		/**
		 * The compression level to use.
		 */
		protected int compressionLevel = -1;

		@Override
		public ImageExporterBuilder name(String name)
		{
			requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();
			this.name = name;
			return this;
		}

		@Override
		public ImageExporterBuilder compressionType(CompressionType type)
		{
			requireThat(type, "type").isNotNull();
			this.compressionType = type;
			return this;
		}

		@Override
		public ImageExporterBuilder compressionLevel(int compressionLevel)
		{
			switch (compressionType)
			{
				case UNCOMPRESSED ->
				{
				}
				case GZIP, ESTARGZ -> requireThat(compressionLevel, "compressionLevel").isBetween(0, 9);
				case ZSTD -> requireThat(compressionLevel, "compressionLevel").isBetween(0, 22);
			}
			this.compressionLevel = compressionLevel;
			return this;
		}
	}

	/**
	 * Builds an exporter that outputs images using the Docker container format.
	 */
	public static final class DefaultDockerImageExporterBuilder extends AbstractImageExporterBuilder
		implements DockerImageExporterBuilder
	{
		private String path;
		private String context = "";

		/**
		 * Creates a DefaultDockerImageExporterBuilder.
		 */
		public DefaultDockerImageExporterBuilder()
		{
		}

		@Override
		public DockerImageExporterBuilder name(String name)
		{
			return (DockerImageExporterBuilder) super.name(name);
		}

		@Override
		public DockerImageExporterBuilder compressionType(CompressionType type)
		{
			return (DockerImageExporterBuilder) super.compressionType(type);
		}

		@Override
		public DockerImageExporterBuilder compressionLevel(int compressionLevel)
		{
			return (DockerImageExporterBuilder) super.compressionLevel(compressionLevel);
		}

		@Override
		public DockerImageExporterBuilder path(String path)
		{
			requireThat(path, "path").doesNotContainWhitespace().isNotEmpty();
			this.path = path;
			return this;
		}

		@Override
		public DockerImageExporterBuilder context(String context)
		{
			requireThat(context, "context").doesNotContainWhitespace().isNotEmpty();
			this.context = context;
			return this;
		}

		@Override
		public Exporter build()
		{
			return new ExporterAdapter();
		}

		private final class ExporterAdapter extends AbstractExporter
		{
			@Override
			public String getType()
			{
				return "docker";
			}

			@Override
			public boolean loadsIntoImageStore()
			{
				return path == null;
			}

			@Override
			public String toCommandLine()
			{
				StringJoiner joiner = new StringJoiner(",");
				joiner.add("type=" + getType());
				if (path != null)
					joiner.add("dest=" + path);
				if (!name.isEmpty())
					joiner.add("name=" + name);
				if (compressionType != CompressionType.GZIP)
					joiner.add("compression=" + compressionType.toCommandLine());
				if (compressionLevel != -1)
					joiner.add("compression-level=" + compressionLevel);
				if (!context.isEmpty())
					joiner.add("context=" + context);
				return joiner.toString();
			}
		}
	}

	/**
	 * Builds an exporter that outputs images to a registry.
	 */
	public static final class DefaultRegistryExporterBuilder extends AbstractImageExporterBuilder
		implements RegistryExporterBuilder
	{
		/**
		 * Creates an DefaultRegistryExporterBuilder.
		 */
		public DefaultRegistryExporterBuilder()
		{
		}

		@Override
		public RegistryExporterBuilder name(String name)
		{
			return (RegistryExporterBuilder) super.name(name);
		}

		@Override
		public RegistryExporterBuilder compressionType(CompressionType type)
		{
			return (RegistryExporterBuilder) super.compressionType(type);
		}

		@Override
		public RegistryExporterBuilder compressionLevel(int compressionLevel)
		{
			return (RegistryExporterBuilder) super.compressionLevel(compressionLevel);
		}

		@Override
		public Exporter build()
		{
			return new DefaultRegistryExporterBuilder.ExporterAdapter();
		}

		private final class ExporterAdapter extends AbstractExporter
		{
			@Override
			public String getType()
			{
				return "registry";
			}

			@Override
			public boolean loadsIntoImageStore()
			{
				return true;
			}

			@Override
			public String toCommandLine()
			{
				StringJoiner joiner = new StringJoiner(",");
				joiner.add("type=registry");
				if (!name.isEmpty())
					joiner.add("name=" + name);
				if (compressionType != CompressionType.GZIP)
					joiner.add("compression=" + compressionType.toCommandLine());
				if (compressionLevel != -1)
					joiner.add("compression-level=" + compressionLevel);
				return joiner.toString();
			}
		}
	}

	/**
	 * Builds an exporter that outputs images to disk using the OCI container format.
	 */
	public static final class DefaultOciImageExporterBuilder extends AbstractImageExporterBuilder
		implements OciImageExporterBuilder
	{
		private final String path;
		private boolean directory;
		private String context = "";

		/**
		 * Creates a OciImageExporterBuilder.
		 * <p>
		 * For multi-platform builds, a separate subdirectory will be created for each platform.
		 * <p>
		 * For example, the directory structure might look like:
		 * <pre>{@code
		 * /
		 * ├── linux_amd64/
		 * └── linux_arm64/
		 * }</pre>
		 *
		 * @param path the output location, which is either a TAR archive or a directory depending on whether
		 *             {@link #directory() directory()} is invoked
		 */
		public DefaultOciImageExporterBuilder(String path)
		{
			requireThat(path, "path").doesNotContainWhitespace().isNotEmpty();
			this.path = path;
		}

		@Override
		public OciImageExporterBuilder name(String name)
		{
			return (OciImageExporterBuilder) super.name(name);
		}

		@Override
		public OciImageExporterBuilder compressionType(CompressionType type)
		{
			return (OciImageExporterBuilder) super.compressionType(type);
		}

		@Override
		public OciImageExporterBuilder compressionLevel(int compressionLevel)
		{
			return (OciImageExporterBuilder) super.compressionLevel(compressionLevel);
		}

		@Override
		public OciImageExporterBuilder directory()
		{
			this.directory = true;
			return this;
		}

		@Override
		public OciImageExporterBuilder context(String context)
		{
			requireThat(context, "context").doesNotContainWhitespace().isNotEmpty();
			this.context = context;
			return this;
		}

		@Override
		public Exporter build()
		{
			return new DefaultOciImageExporterBuilder.ExporterAdapter();
		}

		private final class ExporterAdapter extends AbstractExporter
		{
			@Override
			public String getType()
			{
				return "oci";
			}

			@Override
			public boolean loadsIntoImageStore()
			{
				return false;
			}

			@Override
			public String toCommandLine()
			{
				StringJoiner joiner = new StringJoiner(",");
				joiner.add("type=" + getType());
				if (path != null)
					joiner.add("dest=" + path);
				if (!name.isEmpty())
					joiner.add("name=" + name);
				if (directory)
					joiner.add("tar=false");
				if (compressionType != CompressionType.GZIP)
					joiner.add("compression=" + compressionType.toCommandLine());
				if (compressionLevel != -1)
					joiner.add("compression-level=" + compressionLevel);
				if (!context.isEmpty())
					joiner.add("context=" + context);
				return joiner.toString();
			}
		}
	}
}