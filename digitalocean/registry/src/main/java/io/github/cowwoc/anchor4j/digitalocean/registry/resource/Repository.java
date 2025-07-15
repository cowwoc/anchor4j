package io.github.cowwoc.anchor4j.digitalocean.registry.resource;

import io.github.cowwoc.anchor4j.core.exception.AccessDeniedException;
import io.github.cowwoc.anchor4j.core.id.IntegerId;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/**
 * A repository (collection) of images.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public interface Repository
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the server-side identifier for this resource
	 * @return the ID
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(int value)
	{
		return new Id(value);
	}

	/**
	 * Returns the registry that the repository is in.
	 *
	 * @return the registry
	 */
	Registry getRegistry();

	/**
	 * Returns the repository's name.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the fully qualified name of the repository (e.g.,
	 * {@code registry.digitalocean.com/nasa/rocket-ship}).
	 *
	 * @return the fully qualified name
	 */
	String getFullyQualifiedName();

	/**
	 * Returns the images in the repository.
	 *
	 * @return the images
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<ContainerImage> getImages() throws IOException, InterruptedException;

	/**
	 * Returns the first image that matches a predicate.
	 *
	 * @param predicate the predicate
	 * @return null if no match was found
	 * @throws NullPointerException     if {@code predicate} is null
	 * @throws IllegalArgumentException if any of the tags contain leading or trailing whitespace or are empty
	 * @throws IllegalStateException    if the client is closed
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	ContainerImage getImage(Predicate<ContainerImage> predicate)
		throws IOException, InterruptedException;

	/**
	 * Removes an image and all its tags from the repository.
	 *
	 * @param imageId the image
	 * @throws NullPointerException  if {@code predicate} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 * @throws AccessDeniedException if the client does not have sufficient privileges to execute this request
	 */
	void destroyImage(ContainerImage.Id imageId)
		throws IOException, InterruptedException, AccessDeniedException;

	/**
	 * Deletes all dangling (untagged) images in the registry.
	 *
	 * @throws AccessDeniedException if the client does not have sufficient privileges to execute this request
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	void deleteDanglingImages() throws IOException, InterruptedException, AccessDeniedException;

	/**
	 * A repository ID.
	 */
	final class Id extends IntegerId
	{
		/**
		 * @param value a server-side identifier
		 */
		private Id(int value)
		{
			super(value);
		}
	}
}