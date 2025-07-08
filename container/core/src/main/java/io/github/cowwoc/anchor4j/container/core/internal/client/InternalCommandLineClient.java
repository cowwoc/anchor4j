package io.github.cowwoc.anchor4j.container.core.internal.client;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.cowwoc.anchor4j.core.internal.client.InternalClient;
import io.github.cowwoc.anchor4j.core.internal.client.Operation;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * The internals shared by all command-line clients.
 */
public interface InternalCommandLineClient extends InternalClient
{
	/**
	 * Returns the JSON configuration.
	 *
	 * @return the configuration
	 */
	JsonMapper getJsonMapper();

	/**
	 * Returns a {@code ProcessBuilder} for running a command.
	 *
	 * @param arguments the command-line arguments to pass to the executable
	 * @return the {@code ProcessBuilder}
	 */
	ProcessBuilder getProcessBuilder(List<String> arguments);

	/**
	 * Runs a command and returns its output.
	 *
	 * @param arguments the command-line arguments to pass to the executable
	 * @return the output of the command
	 * @throws NullPointerException if any of the arguments are null
	 * @throws IOException          if the executable could not be found
	 * @throws InterruptedException if the thread was interrupted before the operation completed
	 */
	CommandResult run(List<String> arguments) throws IOException, InterruptedException;

	/**
	 * Runs a command and returns its output.
	 *
	 * @param arguments the command-line arguments to pass to the executable
	 * @param stdin     the bytes to pass into the command's stdin stream
	 * @return the output of the command
	 * @throws NullPointerException if any of the arguments are null
	 * @throws IOException          if the executable could not be found
	 * @throws InterruptedException if the thread was interrupted before the operation completed
	 */
	CommandResult run(List<String> arguments, ByteBuffer stdin) throws IOException, InterruptedException;

	/**
	 * Invoked when a command fails.
	 *
	 * @param result the result of executing a command
	 * @throws IOException if the failure is expected
	 */
	void commandFailed(CommandResult result) throws IOException;

	/**
	 * @return the maximum duration to retry a command that fails due to intermittent {@code IOException}s
	 */
	Duration getRetryTimeout();

	/**
	 * Runs an operation, retrying on intermittent {@code IOException}s.
	 *
	 * @param <V>       the type of value returned by the operation
	 * @param operation the operation
	 * @return the value returned by the operation
	 * @throws IOException          if an I/O error persists beyond the configured retry timeout
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 * @see #retryTimeout(Duration)
	 */
	<V> V retry(Operation<V> operation) throws IOException, InterruptedException;

	/**
	 * Runs an operation, retrying on intermittent {@code IOException}s.
	 *
	 * @param <V>       the type of value returned by the operation
	 * @param operation the operation
	 * @param deadline  the absolute time by which the operation must succeed. The method will retry failed
	 *                  operations while the current time is before this value.
	 * @return the value returned by the operation
	 * @throws IOException          if an I/O error persists beyond the {@code until}
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 * @throws TimeoutException     if the deadline expires before the operation completes successfully, and no
	 *                              other exception was thrown to indicate the failure
	 */
	<V> V retry(Operation<V> operation, Instant deadline)
		throws IOException, InterruptedException, TimeoutException;
}