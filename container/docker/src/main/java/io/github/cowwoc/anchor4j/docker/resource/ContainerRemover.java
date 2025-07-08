package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;

import java.io.IOException;

/**
 * Removes a container.
 */
public interface ContainerRemover
{
	/**
	 * Indicates that the container should be killed (using SIGKILL) if it is running.
	 *
	 * @return this
	 */
	ContainerRemover kill();

	/**
	 * Indicates that any anonymous volumes associated with the container should be automatically removed when
	 * it is deleted.
	 *
	 * @return this
	 */
	ContainerRemover removeAnonymousVolumes();

	/**
	 * Removes the container. If the container does not exist, this method has no effect.
	 *
	 * @throws ResourceInUseException if the container is running and {@link #kill()} was not used
	 * @throws IOException            if an I/O error occurs. These errors are typically transient, and retrying
	 *                                the request may resolve the issue.
	 * @throws InterruptedException   if the thread is interrupted before the operation completes. This can
	 *                                happen due to shutdown signals.
	 */
	void apply() throws IOException, InterruptedException;
}