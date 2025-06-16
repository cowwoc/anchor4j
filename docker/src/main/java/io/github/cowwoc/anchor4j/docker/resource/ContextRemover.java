package io.github.cowwoc.anchor4j.docker.resource;

import java.io.IOException;

/**
 * Removes a context.
 */
public interface ContextRemover
{
	/**
	 * Indicates that the context should be removed even if it is in use by a client.
	 *
	 * @return this
	 */
	ContextRemover force();

	/**
	 * Removes the context. If the context does not exist, this method has no effect.
	 *
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	void remove() throws IOException, InterruptedException;
}