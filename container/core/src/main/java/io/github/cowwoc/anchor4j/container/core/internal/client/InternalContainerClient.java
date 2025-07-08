package io.github.cowwoc.anchor4j.container.core.internal.client;

import io.github.cowwoc.anchor4j.container.core.client.ContainerClient;
import io.github.cowwoc.anchor4j.container.core.internal.resource.BuildXParser;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage.Id;

import java.io.IOException;

/**
 * The internals shared by all clients.
 */
public interface InternalContainerClient extends InternalCommandLineClient, ContainerClient
{
	/**
	 * @return a {@code BuildXParser}
	 */
	BuildXParser getBuildXParser();

	/**
	 * Looks up an image.
	 *
	 * @param id the image's ID or {@link ContainerImage reference}
	 * @return the image
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id}'s format is invalid
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	ContainerImage getImage(String id) throws IOException, InterruptedException;

	/**
	 * Looks up an image.
	 *
	 * @param id the image's ID or {@link ContainerImage reference}
	 * @return the image
	 * @throws NullPointerException if {@code id} is null
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	ContainerImage getImage(Id id) throws IOException, InterruptedException;
}