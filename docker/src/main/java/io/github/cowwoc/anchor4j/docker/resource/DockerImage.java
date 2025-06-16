package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.core.resource.CoreImage;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;

/**
 * A docker image.
 */
public interface DockerImage extends CoreImage
{
	/**
	 * Looks up the image's state.
	 *
	 * @return null if the image does not exist
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	ImageState getState() throws IOException, InterruptedException;

	/**
	 * Creates a container from this image.
	 *
	 * @return a container creator
	 */
	@CheckReturnValue
	ContainerCreator createContainer();

	/**
	 * Adds a new tag to an existing image, creating an additional reference without duplicating image data.
	 * <p>
	 * If the target reference already exists, this method has no effect.
	 *
	 * @param target the new reference to create
	 * @return this
	 * @throws NullPointerException      if {@code target} is null
	 * @throws IllegalArgumentException  if {@code source} or {@code target}'s format are invalid
	 * @throws ResourceNotFoundException if the image does not exist
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	DockerImage tag(String target) throws IOException, InterruptedException;

	/**
	 * Pulls the image from a registry.
	 *
	 * @return an image puller
	 */
	@CheckReturnValue
	ImagePuller puller();

	/**
	 * Pushes the image to a registry.
	 *
	 * @return an image pusher
	 */
	@CheckReturnValue
	ImagePusher pusher() throws IOException, InterruptedException;

	/**
	 * Removes the image.
	 *
	 * @return an image remover
	 */
	@CheckReturnValue
	ImageRemover remover();
}