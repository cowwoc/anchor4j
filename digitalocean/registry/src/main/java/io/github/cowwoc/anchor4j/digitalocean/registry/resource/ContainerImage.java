package io.github.cowwoc.anchor4j.digitalocean.registry.resource;

import io.github.cowwoc.anchor4j.core.exception.AccessDeniedException;
import io.github.cowwoc.anchor4j.core.id.StringId;
import io.github.cowwoc.anchor4j.digitalocean.core.exception.TooManyRequestsException;

import java.io.IOException;
import java.util.Set;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A container image used for virtualization workloads.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public interface ContainerImage
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the server-side identifier
	 * @return the type-safe identifier for the resource
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(String value)
	{
		if (value == null)
			return null;
		return new Id(value);
	}

	/**
	 * Returns the repository that the image is in.
	 *
	 * @return the repository
	 */
	Repository getRepository();

	/**
	 * Returns the image's ID. An ID uniquely identifies the image within a repository.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * Returns the image's tags.
	 *
	 * @return the tags
	 */
	Set<String> getTags();

	/**
	 * Returns the layers that are used by the image.
	 *
	 * @return the layers
	 */
	Set<Layer> getLayers();

	/**
	 * Reloads the image's state.
	 *
	 * @return the updated image
	 * @throws IllegalStateException    if the client is closed
	 * @throws TooManyRequestsException if the client has sent too many requests in a given period and has
	 *                                  surpassed the server's rate limit
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	ContainerImage reload() throws IOException, InterruptedException;

	/**
	 * Removes an image and all its tags.
	 *
	 * @throws IllegalStateException    if the client is closed
	 * @throws AccessDeniedException    if the client does not have sufficient privileges to execute this
	 *                                  request
	 * @throws TooManyRequestsException if the client has sent too many requests in a given period and has
	 *                                  surpassed the server's rate limit
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	void destroy() throws IOException, InterruptedException, AccessDeniedException;

	/**
	 * An image ID. An ID uniquely identifies the image within a repository.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value a server-side identifier
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
		 */
		private Id(String value)
		{
			super(value);
		}
	}

	/**
	 * A layer of the image.
	 *
	 * @param id the image that is referenced by the layer
	 */
	record Layer(Id id)
	{
		/**
		 * Creates a new Layer.
		 *
		 * @param id the image that is referenced by the layer
		 * @throws NullPointerException if {@code id} is null
		 */
		public Layer
		{
			requireThat(id, "id").isNotNull();
		}
	}
}