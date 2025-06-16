package io.github.cowwoc.anchor4j.docker.resource;

/**
 * A docker network.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
@FunctionalInterface
public interface Network
{
	/**
	 * Returns the ID of the network.
	 *
	 * @return the ID
	 */
	String getId();
}