package io.github.cowwoc.anchor4j.digitalocean.compute.resource;

import io.github.cowwoc.anchor4j.core.id.IntegerId;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;

import java.time.Instant;
import java.util.Set;

/**
 * The system image that was used to boot a droplet.
 */
public interface DropletImage
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
	 * Returns the ID of the image.
	 *
	 * @return the ID of the image
	 */
	Id getId();

	/**
	 * Returns the slug of the image.
	 *
	 * @return the slug of the image
	 */
	String getSlug();

	/**
	 * Returns the name of the image.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the OS distribution of the image.
	 *
	 * @return the OS distribution
	 */
	String getDistribution();

	/**
	 * Determines if the image is available for public use.
	 *
	 * @return {@code true} if the image is available for public use
	 */
	boolean isPublic();

	/**
	 * Returns the regions that the image is available in.
	 *
	 * @return the regions
	 */
	Set<Region.Id> getRegions();

	/**
	 * Returns the type of the image.
	 *
	 * @return the type
	 */
	Type getType();

	/**
	 * Returns the minimum disk size in GiB required to use this image.
	 *
	 * @return the minimum disk size
	 */
	int getMinDiskSizeInGiB();

	/**
	 * Returns the size of the image in GiB.
	 *
	 * @return the size
	 */
	int getSizeInGiB();

	/**
	 * Returns a description of the image.
	 *
	 * @return the description
	 */
	String getDescription();

	/**
	 * Returns the tags that are associated with the image.
	 *
	 * @return the tags
	 */
	Set<String> getTags();

	/**
	 * Returns the status of the image.
	 *
	 * @return the status
	 */
	Status getStatus();

	/**
	 * Returns an explanation of why the image cannot be used.
	 *
	 * @return an explanation of the failure
	 */
	String getErrorMessage();

	/**
	 * Returns the time the image was created at.
	 *
	 * @return the time
	 */
	Instant getCreatedAt();

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

	/**
	 * The image of the image.
	 */
	enum Type
	{
		/**
		 * Base OS image.
		 */
		BASE,
		/**
		 * User-generated Droplet snapshot.
		 */
		SNAPSHOT,
		/**
		 * Automatically created Droplet backup.
		 */
		BACKUP,
		/**
		 * User-provided virtual machine image.
		 */
		CUSTOM,
		/**
		 * Image used by DigitalOcean managed resources (e.g. DOKS worker nodes).
		 */
		ADMIN
	}

	/**
	 * The status of the image.
	 */
	enum Status
	{
		// Per DigitalOcean tech support, the "pending" value is never returned.
		/**
		 * The image is being processed by DigitalOcean.
		 */
		NEW,
		/**
		 * The image is available for use.
		 */
		AVAILABLE,
		/**
		 * The image is no longer actively supported or maintained.
		 */
		RETIRED,
		/**
		 * The image has been deleted and is no longer available for use.
		 */
		DELETED
	}
}