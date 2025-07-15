package io.github.cowwoc.anchor4j.digitalocean.compute.resource;

import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;

import java.util.Set;

/**
 * A Region with additional information specific to compute resources.
 */
public interface ComputeRegion extends Region
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
	 * Determines if new droplets can be created in this region.
	 *
	 * @return {@code true} if new droplets can be created
	 */
	boolean canCreateDroplets();

	/**
	 * Returns the droplet types that can be created in this region.
	 *
	 * @return the droplet types
	 */
	Set<DropletType.Id> getDropletTypes();

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
		 * Ability to install a metrics agent on droplets.
		 */
		INSTALL_AGENT,
		/**
		 * Ability to use the Spaces and Block Storage Volume services.
		 */
		STORAGE,
		/**
		 * Ability to use custom droplet images within this region.
		 */
		IMAGE_TRANSFER
	}
}