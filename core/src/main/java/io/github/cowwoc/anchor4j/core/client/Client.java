package io.github.cowwoc.anchor4j.core.client;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

/**
 * Code common to all clients.
 */
public interface Client extends AutoCloseable
{
	/**
	 * Sets the maximum duration to retry a command that fails due to intermittent {@code IOException}s. The
	 * default is 10 seconds.
	 * <p>
	 * If the timeout is exceeded, the command fails with the last encountered {@code IOException}.
	 *
	 * @param duration the timeout
	 * @return this
	 * @throws NullPointerException if {@code duration} is null
	 */
	Client retryTimeout(Duration duration);

	/**
	 * Looks up the resources that match the specified filters.
	 *
	 * @param typeFilter     a function that returns {@code true} for matching resource types
	 * @param resourceFilter a function that returns {@code true} for matching resources
	 * @return an empty List if no match is found
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<Object> getResources(Predicate<? super Class<?>> typeFilter, Predicate<Object> resourceFilter)
		throws IOException, InterruptedException;

	/**
	 * Closes the client.
	 */
	@Override
	void close();
}