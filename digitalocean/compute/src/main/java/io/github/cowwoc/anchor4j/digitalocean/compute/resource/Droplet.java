package io.github.cowwoc.anchor4j.digitalocean.compute.resource;

import io.github.cowwoc.anchor4j.core.id.IntegerId;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Set;

/**
 * A computer node.
 */
public interface Droplet
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the server-side identifier
	 * @return the type-safe identifier for the resource
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(int value)
	{
		return new Id(value);
	}

	/**
	 * Returns the name of the droplet.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the machine type of the droplet.
	 *
	 * @return the machine type
	 */
	DropletType.Id getType();

	/**
	 * Returns the image used to boot this droplet.
	 *
	 * @return the image
	 */
	DropletImage getImage();

	/**
	 * Returns the region that the droplet is deployed in.
	 *
	 * @return the region
	 */
	Region.Id getRegion();

	/**
	 * Returns the VPC that the droplet is deployed in.
	 *
	 * @return the VPC
	 */
	Vpc.Id getVpc();

	/**
	 * Returns the droplet's IP addresses.
	 *
	 * @return the IP addresses
	 */
	Set<InetAddress> getAddresses();

	/**
	 * Returns the features that are enabled on the droplet.
	 *
	 * @return the features that are enabled
	 */
	Set<DropletFeature> getFeatures();

	/**
	 * Returns the droplet's tags.
	 *
	 * @return the tags
	 */
	Set<String> getTags();

	/**
	 * Returns the time the droplet was created.
	 *
	 * @return the time
	 */
	Instant getCreatedAt();

	/**
	 * Reloads the droplet's state.
	 *
	 * @return the updated state
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	@CheckReturnValue
	Droplet reload() throws IOException, InterruptedException;

	/**
	 * Renames a droplet.
	 *
	 * @param newName the new name
	 * @return the updated droplet
	 * @throws NullPointerException     if {@code newName} is null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>the {@code name} contains any characters other than {@code A-Z},
	 *                                    {@code a-z}, {@code 0-9} and a hyphen.</li>
	 *                                    <li>the {@code name} does not start or end with an alphanumeric
	 *                                    character.</li>
	 *                                    <li>any of the arguments contain leading or trailing whitespace or
	 *                                    are empty.</li>
	 *                                  </ul>
	 * @throws NullPointerException     if {@code newName} is null
	 * @throws IllegalArgumentException if {@code newName} contains leading or trailing whitespace or is empty
	 * @throws IllegalStateException    if the client is closed
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	Droplet renameTo(String newName) throws IOException, InterruptedException;

	/**
	 * Destroys the droplet.
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