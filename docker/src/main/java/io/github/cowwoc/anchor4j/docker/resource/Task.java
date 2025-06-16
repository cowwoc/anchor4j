package io.github.cowwoc.anchor4j.docker.resource;

/**
 * A unit of work (container) that executes on a Swarm node.
 * <p>
 * <b>Thread Safety</b>: This class is immutable and thread-safe.
 */
@FunctionalInterface
public interface Task
{
	/**
	 * Returns the task's ID.
	 *
	 * @return the ID
	 */
	String getId();
}