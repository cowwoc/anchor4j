package io.github.cowwoc.anchor4j.container.core.internal.client;

import io.github.cowwoc.anchor4j.container.core.client.ContainerClient;
import io.github.cowwoc.anchor4j.container.core.internal.parser.BuildXParser;
import io.github.cowwoc.anchor4j.container.core.internal.resource.DefaultBuilderCreator;
import io.github.cowwoc.anchor4j.container.core.internal.resource.DefaultContainerImage;
import io.github.cowwoc.anchor4j.container.core.internal.resource.DefaultContainerImageBuilder;
import io.github.cowwoc.anchor4j.container.core.resource.Builder;
import io.github.cowwoc.anchor4j.container.core.resource.Builder.Node.Status;
import io.github.cowwoc.anchor4j.container.core.resource.BuilderCreator;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImageBuilder;
import io.github.cowwoc.anchor4j.core.internal.util.Processes;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.regex.Matcher;

import static io.github.cowwoc.anchor4j.container.core.internal.parser.AbstractContainerParser.ACCESS_DENIED;
import static io.github.cowwoc.anchor4j.container.core.internal.parser.AbstractContainerParser.CONNECTION_RESET;
import static io.github.cowwoc.anchor4j.container.core.internal.parser.AbstractContainerParser.FILE_LOCKED;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Common implementation shared by all {@code InternalClient}s.
 */
public abstract class AbstractInternalContainerClient extends AbstractInternalCommandLineClient
	implements InternalContainerClient
{
	@SuppressWarnings("this-escape")
	private final BuildXParser buildXParser = new BuildXParser(this);

	/**
	 * Creates an AbstractInternalContainer.
	 *
	 * @param executable the path of the command-line executable
	 * @throws NullPointerException     if {@code executable} is null
	 * @throws IllegalArgumentException if the path referenced by {@code executable} does not exist or is not an
	 *                                  executable file
	 * @throws IOException              if an I/O error occurs while reading {@code executable}'s attributes
	 */
	protected AbstractInternalContainerClient(Path executable) throws IOException
	{
		super(executable);
	}

	@Override
	public ContainerClient retryTimeout(Duration duration)
	{
		return (ContainerClient) super.retryTimeout(duration);
	}

	@Override
	public void commandFailed(CommandResult result) throws IOException
	{
		String stderr = result.stderr();
		Matcher matcher = CONNECTION_RESET.matcher(stderr);
		if (matcher.matches())
			throw new IOException("Connection reset trying to connect to " + matcher.group(1));
		if (!Processes.isWindows())
			return;
		matcher = FILE_LOCKED.matcher(stderr);
		if (matcher.matches())
			throw new IOException("File locked by another process: " + matcher.group(1));
		matcher = ACCESS_DENIED.matcher(stderr);
		if (matcher.matches())
			throw new IOException("File locked by another process: " + matcher.group(1));
	}

	@Override
	public BuildXParser getBuildXParser()
	{
		return buildXParser;
	}

	@Override
	public Builder getDefaultBuilder() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/buildx/inspect/
		List<String> arguments = new ArrayList<>(2);
		arguments.add("buildx");
		arguments.add("inspect");
		CommandResult result = retry(_ -> run(arguments));
		return getBuildXParser().builderFromServer(result);
	}

	@Override
	public BuilderCreator createBuilder()
	{
		return new DefaultBuilderCreator(this);
	}

	@Override
	public List<Builder> getBuilders() throws IOException, InterruptedException
	{
		return getBuilders(_ -> true);
	}

	@Override
	public List<Builder> getBuilders(Predicate<Builder> predicate) throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/buildx/inspect/
		List<String> arguments = List.of("buildx", "ls", "--format", "json");
		CommandResult result = retry(_ -> run(arguments));
		return getBuildXParser().getBuilders(result).stream().filter(predicate).toList();
	}

	@Override
	public Builder getBuilder(Builder.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.docker.com/reference/cli/docker/buildx/inspect/
		List<String> arguments = new ArrayList<>(3);
		arguments.add("buildx");
		arguments.add("inspect");
		arguments.add(id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		return getBuildXParser().builderFromServer(result);
	}

	@Override
	public Builder getBuilder(Predicate<Builder> predicate) throws IOException, InterruptedException
	{
		List<Builder> builders = getBuilders(predicate);
		if (builders.isEmpty())
			return null;
		return builders.getFirst();
	}

	@Override
	@SuppressWarnings("BusyWait")
	public Builder waitUntilBuilderStatus(Builder.Id id, Builder.Node.Status status, Instant deadline)
		throws IOException, InterruptedException, TimeoutException
	{
		while (true)
		{
			Builder builder = getBuilder(id);

			Instant now = Instant.now();
			if (builder == null)
			{
				log.debug("builder == null");
				if (now.isAfter(deadline))
					throw new TimeoutException("Builder not found");
			}
			else if (builder.getNodes().isEmpty())
			{
				log.debug("builder.getNodes() is empty");
				if (now.isAfter(deadline))
					throw new TimeoutException("Builder not found");
			}
			else
			{
				Builder.Node firstNode = builder.getNodes().getFirst();
				if (firstNode.getStatus() == status)
					return builder;
				log.debug("builder.status: {}", firstNode.getStatus());
				if (now.isAfter(deadline))
				{
					StringBuilder message = new StringBuilder("Default builder " + builder.getName() +
						" has a state of " + firstNode.getStatus());
					if (firstNode.getStatus() == Status.ERROR)
					{
						message.append("\n" +
							"Error: ").append(firstNode.getError());
					}
					throw new TimeoutException(message.toString());
				}
			}
			Thread.sleep(100);
		}
	}

	@Override
	public Set<String> getSupportedBuildPlatforms() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/buildx/inspect/
		List<String> arguments = List.of("buildx", "inspect");
		CommandResult result = retry(_ -> run(arguments));
		return getBuildXParser().getSupportedBuildPlatforms(result);
	}

	@Override
	public ContainerImage getImage(String id) throws IOException, InterruptedException
	{
		return getImage(ContainerImage.id(id));
	}

	@Override
	public ContainerImage getImage(ContainerImage.Id id) throws IOException, InterruptedException
	{
		return new DefaultContainerImage(id);
	}

	@Override
	public ContainerImageBuilder buildImage()
	{
		return new DefaultContainerImageBuilder(this);
	}
}