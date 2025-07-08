package io.github.cowwoc.anchor4j.digitalocean.database.resource;

/**
 * Represents a type of resource.
 */
public enum ResourceType
{
	/**
	 * A droplet.
	 */
	DROPLET,
	/**
	 * A Kubernetes cluster.
	 */
	KUBERNETES,
	/**
	 * An IP address.
	 */
	IP_ADDRESS,
	/**
	 * A resource tag.
	 */
	TAG,
	/**
	 * An application (app platform).
	 */
	APPLICATION
}