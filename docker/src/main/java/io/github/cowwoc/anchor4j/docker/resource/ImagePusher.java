package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;

import java.io.IOException;

/**
 * Pushes an image to a registry.
 */
public interface ImagePusher
{
	/**
	 * Sets the platform to push. By default, all platforms are pushed.
	 *
	 * @param platform the platform of the image, or an empty string to push all platforms
	 * @return this
	 * @throws NullPointerException     if {@code platform} is null
	 * @throws IllegalArgumentException if {@code platform} contains whitespace
	 */
	ImagePusher platform(String platform);

	/**
	 * Pushes the image to a registry.
	 *
	 * @return the image
	 * @throws ResourceNotFoundException if the referenced image could not be found
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 * @see Docker#login(String, String, String)
	 */
	DockerImage push() throws IOException, InterruptedException;
}