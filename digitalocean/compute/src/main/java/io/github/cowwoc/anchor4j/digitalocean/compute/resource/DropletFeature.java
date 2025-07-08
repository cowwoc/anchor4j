package io.github.cowwoc.anchor4j.digitalocean.compute.resource;

/**
 * Droplet features.
 */
public enum DropletFeature
{
	/**
	 * System-level backups at weekly or daily intervals.
	 */
	BACKUPS,
	/**
	 * Ensures network communication is isolated within the account or team where the Droplets were created.
	 */
	PRIVATE_NETWORKING,
	/**
	 * Network communication over IPv6.
	 */
	IPV6,
	/**
	 * Provides additional insights into a Droplet's resource utilization, including load average, finer
	 * granularity for CPU usage, memory usage, disk I/O, and network bandwidth. It offers comprehensive
	 * performance metrics and alerting capabilities for the Droplet.
	 */
	MONITORING,
	/**
	 * Enables the dynamic reassignment of IP addresses between Droplets, ensuring rapid failover and high
	 * availability. If a primary instance fails, the floating IP is automatically reassigned to a standby
	 * instance, ensuring continuous service availability without requiring changes on the client side.
	 */
	FLOATING_IPS,
	/**
	 * Indicates that the Droplet is assigned to a firewall, enhancing its network security. This provides
	 * protection by applying predefined rules to control incoming and outgoing traffic.
	 */
	FIREWALLS,
	/**
	 * Indicates that load balancing is enabled for the Droplet, distributing traffic across multiple Droplets
	 * for optimal performance and reliability.
	 */
	LOAD_BALANCERS,
	/**
	 * Indicates that Spaces (object storage) services are assigned to the Droplet, allowing scalable storage
	 * solutions.
	 */
	SPACES,
	/**
	 * Indicates that block storage volumes are assigned to the droplet for increased storage capacity.
	 */
	VOLUMES
}