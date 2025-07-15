package io.github.cowwoc.anchor4j.digitalocean.network.resource;

import io.github.cowwoc.anchor4j.core.id.StringId;
import io.github.cowwoc.anchor4j.core.internal.util.Strings;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Represents a private network, also known as a Virtual Private Cloud (VPC).
 * <p>
 * VPCs are region-specific, meaning they cannot secure communication between droplets located in different
 * regions.
 *
 * @see <a href="https://youtu.be/nbo5HrmZjXo?si=5Gf4hbticjw6nwph&t=2140">Configure a VPN to Extend a VPC
 * 	Across Regions</a>
 * @see <a
 * 	href="https://www.digitalocean.com/community/developer-center/connect-digitalocean-droplets-across-regions">
 * 	Connect DigitalOcean Droplets Across Regions</a>
 * @see <a href="https://www.digitalocean.com/blog/vpc-peering-ga">VPC Peering</a>
 */
public interface Vpc
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
	 * Returns the VPC's ID.
	 *
	 * @return the id
	 */
	Id getId();

	/**
	 * Returns the region that the VPC is in.
	 *
	 * @return the region
	 */
	Region.Id getRegion();

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value a server-side identifier
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value} is not a valid UUID
		 */
		private Id(String value)
		{
			super(value);
			requireThat(value, "value").matches(Strings.UUID);
		}
	}
}