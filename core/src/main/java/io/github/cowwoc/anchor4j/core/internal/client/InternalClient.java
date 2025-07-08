package io.github.cowwoc.anchor4j.core.internal.client;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.cowwoc.anchor4j.core.client.Client;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

/**
 * The internals shared by all clients.
 */
public interface InternalClient extends Client
{
	/**
	 * Returns the JSON configuration.
	 *
	 * @return the configuration
	 */
	JsonMapper getJsonMapper();

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