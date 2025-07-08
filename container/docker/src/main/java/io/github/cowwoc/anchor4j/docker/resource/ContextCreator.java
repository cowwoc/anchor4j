package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;

import java.io.IOException;

/**
 * Creates a context.
 */
public interface ContextCreator
{
	/**
	 * Sets the context's description.
	 *
	 * @param description the description, or an empty string to omit
	 * @return this
	 * @throws NullPointerException     if {@code description} is null
	 * @throws IllegalArgumentException if {@code description} contains whitespace
	 */
	ContextCreator description(String description);

	/**
	 * Creates the context.
	 *
	 * @return the new context
	 * @throws ResourceNotFoundException if any of the {@link ContextEndpoint referenced TLS files} is not
	 *                                   found
	 * @throws ResourceInUseException    if another context with the same name already exists
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	Context apply() throws IOException, InterruptedException;
}