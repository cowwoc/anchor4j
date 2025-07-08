package io.github.cowwoc.anchor4j.container.core.internal.client;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.cowwoc.anchor4j.core.internal.client.AbstractInternalClient;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Common implementation shared by all command-line clients.
 */
public abstract class AbstractInternalCommandLineClient extends AbstractInternalClient
	implements InternalCommandLineClient
{
	/**
	 * The path of the command-line executable.
	 */
	protected final Path executable;
	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	/**
	 * Creates an AbstractCommandLineInternalClient.
	 *
	 * @param executable the path of the command-line executable
	 * @throws NullPointerException     if {@code executable} is null
	 * @throws IllegalArgumentException if the path referenced by {@code executable} does not exist or is not an
	 *                                  executable file
	 * @throws IOException              if an I/O error occurs while reading {@code executable}'s attributes
	 */
	protected AbstractInternalCommandLineClient(Path executable) throws IOException
	{
		requireThat(executable, "executable").exists().isRegularFile().isExecutable();
		this.executable = executable;
	}

	@Override
	public JsonMapper getJsonMapper()
	{
		return jsonMapper;
	}

	@Override
	public CommandResult run(List<String> arguments) throws IOException, InterruptedException
	{
		return new CommandRunner(getProcessBuilder(arguments)).
			failureHandler(this::commandFailed).
			apply();
	}

	@Override
	public CommandResult run(List<String> arguments, ByteBuffer stdin) throws IOException, InterruptedException
	{
		return new CommandRunner(getProcessBuilder(arguments)).
			stdin(stdin).
			failureHandler(this::commandFailed).
			apply();
	}
}