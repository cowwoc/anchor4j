package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;

import java.io.IOException;

/**
 * Pulls an image from a registry.
 */
public interface ImagePuller
{
	/**
	 * Sets the platform to pull.
	 *
	 * @param platform the platform of the image
	 * @return this
	 * @throws NullPointerException     if {@code platform} is null
	 * @throws IllegalArgumentException if {@code platform} contains whitespace or is empty
	 */
	ImagePuller platform(String platform);

	/**
	 * Pulls the image from a registry.
	 *
	 * @return the image
	 * @throws ResourceNotFoundException if the image does not exist or may require {@code docker login}
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 * @see DockerClient#login(String, String, String)
	 */
	DockerImage apply() throws IOException, InterruptedException;
}