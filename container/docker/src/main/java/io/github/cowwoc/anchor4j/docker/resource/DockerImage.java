package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * A docker image.
 */
public interface DockerImage extends ContainerImage
{
	/**
	 * Returns a mapping of an image's name to its associated tags.
	 * <p>
	 * Locally, an image might have a name such as {@code nasa/rocket-ship} with tags {@code {"1.0", "latest"}},
	 * all referring to the same revision. In a registry, the same image could have a fully qualified name like
	 * {@code docker.io/nasa/rocket-ship} and be associated with multiple tags, such as
	 * {@code {"1.0", "2.0", "latest"}}, all referring to the same revision.
	 *
	 * @return an empty map if the image has no tags
	 */
	Map<String, Set<String>> referenceToTags();

	/**
	 * Returns a mapping of an image's name on registries to its associated digest.
	 * <p>
	 * For example, an image might have a name such as {@code docker.io/nasa/rocket-ship} with digest
	 * {@code "sha256:afcc7f1ac1b49db317a7196c902e61c6c3c4607d63599ee1a82d702d249a0ccb"}.
	 *
	 * @return an empty map if the image has not been pushed to any repositories
	 */
	Map<String, String> referenceToDigest();

	/**
	 * Reloads the image.
	 *
	 * @return the updated image
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@CheckReturnValue
	DockerImage reload() throws IOException, InterruptedException;

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
	 * @param reference the new reference to create
	 * @return this
	 * @throws NullPointerException      if {@code reference} is null
	 * @throws IllegalArgumentException  if {@link ContainerImage reference}'s format is invalid
	 * @throws ResourceNotFoundException if the image does not exist
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	DockerImage addTag(String reference) throws IOException, InterruptedException;

	/**
	 * Removes an image's tag. If the tag is the only one for the image, both the image and the tag are
	 * removed.
	 *
	 * @param reference the reference to remove
	 * @return an image remover
	 * @throws IllegalArgumentException if {@link ContainerImage reference}'s format is invalid
	 */
	@CheckReturnValue
	ImageRemover removeTag(String reference);

	/**
	 * Removes an image and all of its tags.
	 *
	 * @return an image remover
	 */
	@CheckReturnValue
	ImageRemover remove();
}