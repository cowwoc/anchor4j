package io.github.cowwoc.anchor4j.docker.resource;

/**
 * Represents a Docker context (i.e., the Docker Engine that the client communicates with).
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 *
 * @see <a href="https://docs.docker.com/engine/manage-resources/contexts/">Docker documentation</a>
 */
@FunctionalInterface
public interface Context
{
	/**
	 * Returns the context's name.
	 *
	 * @return the name
	 */
	String getName();
}