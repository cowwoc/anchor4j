package io.github.cowwoc.anchor4j.digitalocean.database.resource;

import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;

import java.util.Set;

/**
 * A Region with additional information specific to database resources.
 */
public interface DatabaseRegion extends Region
{
	/**
	 * Returns the region's name.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the features that are available in this region.
	 *
	 * @return the features
	 */
	Set<Feature> getFeatures();

	/**
	 * Determines if new Droplets can be created in this region.
	 *
	 * @return {@code true} if new Droplets can be created
	 */
	boolean canCreateDroplets();

	/**
	 * Region features.
	 */
	enum Feature
	{
		/**
		 * System-level backups at weekly or daily intervals.
		 */
		BACKUPS,
		/**
		 * Network communication over IPv6.
		 */
		IPV6,
		/**
		 * The metadata service, allowing droplets to query metadata from within the droplet itself.
		 */
		METADATA,
		/**
		 * Ability to install a metrics agent on Droplets.
		 */
		INSTALL_AGENT,
		/**
		 * Ability to use the Spaces and Block Storage Volume services.
		 */
		STORAGE,
		/**
		 * Ability to use custom Droplet images within this region.
		 */
		IMAGE_TRANSFER
	}
}