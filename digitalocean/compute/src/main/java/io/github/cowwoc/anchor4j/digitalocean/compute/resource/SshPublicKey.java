package io.github.cowwoc.anchor4j.digitalocean.compute.resource;

import io.github.cowwoc.anchor4j.core.id.IntegerId;

import java.io.IOException;

/**
 * An SSH public key that is registered with the account or team where they were created.
 */
public interface SshPublicKey
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the server-side identifier
	 * @return the type-safe identifier for the resource
	 */
	static Id id(int value)
	{
		return new Id(value);
	}

	/**
	 * Returns the ID of the public key.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * Returns the name of the public key.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the fingerprint of the public key.
	 *
	 * @return the fingerprint
	 */
	String getFingerprint();

	/**
	 * Destroys the SSH key.
	 *
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	void destroy() throws IOException, InterruptedException;

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
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