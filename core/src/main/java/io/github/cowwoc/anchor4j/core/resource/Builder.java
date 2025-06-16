package io.github.cowwoc.anchor4j.core.resource;

import java.io.IOException;

/**
 * Represents a service that builds images.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public interface Builder
{
	/**
	 * Returns the builder's name.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Looks up the builder's state.
	 *
	 * @return null if the builder does not exist
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	BuilderState getState() throws IOException, InterruptedException;
}