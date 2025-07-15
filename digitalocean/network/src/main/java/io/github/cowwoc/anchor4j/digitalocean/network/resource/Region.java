package io.github.cowwoc.anchor4j.digitalocean.network.resource;

/**
 * A geographical region that contains one or more <a
 * href="https://docs.digitalocean.com/platform/regional-availability/">datacenters</a>.
 */
public interface Region
{
	/**
	 * Returns this region's ID.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	enum Id
	{
		/**
		 * Atlanta, United States, data center 1.
		 */
		ATLANTA1,
		/**
		 * New York, United States, data center 1.
		 */
		NEW_YORK1,
		/**
		 * New York, United States, data center 2.
		 */
		NEW_YORK2,
		/**
		 * New York, United States, data center 3.
		 */
		NEW_YORK3,
		/**
		 * San Francisco, United States, data center 1.
		 */
		SAN_FRANCISCO1,
		/**
		 * San Francisco, United States, data center 2.
		 */
		SAN_FRANCISCO2,
		/**
		 * San Francisco, United States, data center 3.
		 */
		SAN_FRANCISCO3,
		/**
		 * Amsterdam, Netherlands, data center 2.
		 */
		AMSTERDAM2,
		/**
		 * Amsterdam, Netherlands, data center 3.
		 */
		AMSTERDAM3,
		/**
		 * Singapore, Singapore, data center 1.
		 */
		SINGAPORE1,
		/**
		 * London, United Kingdom, data center 1.
		 */
		LONDON1,
		/**
		 * Frankfurt, Germany, data center 1.
		 */
		FRANCE1,
		/**
		 * Toronto, Canada, data center 1.
		 */
		TORONTO1,
		/**
		 * Bangalore, India, data center 1.
		 */
		BANGALORE1,
		/**
		 * Sydney, Australia, data center 1.
		 */
		SYDNEY1;
	}
}