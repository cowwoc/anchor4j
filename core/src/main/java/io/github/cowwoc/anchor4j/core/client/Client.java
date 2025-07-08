package io.github.cowwoc.anchor4j.core.client;

import java.time.Duration;

/**
 * Code common to all clients.
 */
public interface Client
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
}