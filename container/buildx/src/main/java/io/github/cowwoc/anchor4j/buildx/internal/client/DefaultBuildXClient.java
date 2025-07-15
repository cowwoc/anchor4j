package io.github.cowwoc.anchor4j.buildx.internal.client;

import io.github.cowwoc.anchor4j.buildx.client.BuildXClient;
import io.github.cowwoc.anchor4j.container.core.internal.client.AbstractInternalContainerClient;
import io.github.cowwoc.anchor4j.container.core.internal.client.InternalContainerClient;
import io.github.cowwoc.anchor4j.container.core.resource.Builder;
import io.github.cowwoc.anchor4j.core.internal.util.Paths;
import io.github.cowwoc.pouch.core.ConcurrentLazyReference;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class DefaultBuildXClient extends AbstractInternalContainerClient
	implements InternalContainerClient, BuildXClient
{
	private static final ConcurrentLazyReference<Path> EXECUTABLE_FROM_PATH = ConcurrentLazyReference.create(
		() ->
		{
			Path path = Paths.searchPath(List.of("buildx", "docker-buildx"));
			if (path == null)
				path = Paths.searchPath(List.of("docker"));
			if (path == null)
				throw new UncheckedIOException(new IOException("Could not find buildx or docker on the PATH"));
			return path;
		});

	/**
	 * @return the path of the {@code buildx} or {@code docker} executable located in the {@code PATH}
	 * 	environment variable
	 */
	private static Path getExecutableFromPath() throws IOException
	{
		try
		{
			return EXECUTABLE_FROM_PATH.getValue();
		}
		catch (UncheckedIOException e)
		{
			throw e.getCause();
		}
	}

	private final boolean executableIsBuildX;

	/**
	 * Creates a client that uses the {@code buildx} executable located in the {@code PATH} environment
	 * variable.
	 *
	 * @throws IOException if an I/O error occurs while building the client
	 */
	public DefaultBuildXClient() throws IOException
	{
		this(getExecutableFromPath());
	}

	/**
	 * Returns a client.
	 *
	 * @param executable the path of the {@code buildx} executable
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the path referenced by {@code executable} does not exist or is not an
	 *                                  executable file
	 */
	private DefaultBuildXClient(Path executable) throws IOException
	{
		super(executable);
		String filename = executable.getFileName().toString();
		this.executableIsBuildX = filename.contains("buildx");
	}

	@Override
	public BuildXClient retryTimeout(Duration duration)
	{
		return (BuildXClient) super.retryTimeout(duration);
	}

	@Override
	public ProcessBuilder getProcessBuilder(List<String> arguments)
	{
		List<String> command = new ArrayList<>(arguments.size() + 3);
		if (executableIsBuildX)
		{
			// Remove "buildx" from the arguments as it will be replaced by the executable
			arguments = arguments.subList(1, arguments.size());
		}
		command.add(executable.toString());
		command.addAll(arguments);
		return new ProcessBuilder(command);
	}

	@Override
	public List<Object> getResources(Predicate<? super Class<?>> typeFilter, Predicate<Object> resourceFilter)
		throws IOException, InterruptedException
	{
		Set<Class<?>> types = Set.of(Builder.class).stream().filter(typeFilter).collect(Collectors.toSet());
		if (types.isEmpty())
			return List.of();
		return List.copyOf(getBuilders(resourceFilter::test));
	}

	@Override
	public void close()
	{
	}
}