package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;

import java.io.IOException;

/**
 * Removes an image.
 */
public interface ImageRemover
{
	/**
	 * Indicates that the image should be removed even if it is tagged in multiple repositories.
	 *
	 * @return this
	 */
	ImageRemover force();

	/**
	 * Prevents automatic removal of untagged parent images when this image is removed.
	 *
	 * @return this
	 */
	ImageRemover doNotPruneParents();

	/**
	 * Removes the image. If the image does not exist, this method has no effect.
	 *
	 * @throws ResourceInUseException if the image is tagged in multiple repositories or in use by containers
	 *                                and {@link #force()} was not used
	 * @throws IOException            if an I/O error occurs. These errors are typically transient, and retrying
	 *                                the request may resolve the issue.
	 * @throws InterruptedException   if the thread is interrupted before the operation completes. This can
	 *                                happen due to shutdown signals.
	 */
	void remove() throws IOException, InterruptedException;
}