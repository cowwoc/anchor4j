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
		 * New York, United States.
		 */
		NEW_YORK,
		/**
		 * Amsterdam, Netherlands.
		 */
		AMSTERDAM,
		/**
		 * San Francisco, United States.
		 */
		SAN_FRANCISCO,
		/**
		 * Singapore, Singapore.
		 */
		SINGAPORE,
		/**
		 * London, United Kingdom.
		 */
		LONDON,
		/**
		 * Frankfurt, Germany.
		 */
		FRANCE,
		/**
		 * Toronto, Canada.
		 */
		TORONTO,
		/**
		 * Bangalore, India.
		 */
		BANGALORE,
		/**
		 * Sydney, Australia.
		 */
		SYDNEY
	}
}