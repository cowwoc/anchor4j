package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.core.id.StringId;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;

import java.io.IOException;
import java.util.List;

/**
 * Represents a high-level definition of a task or application you want to run in a Swarm. It defines:
 * <ul>
 * <li>The desired state, such as the number of replicas (containers).</li>
 * <li>The container image to use.</li>
 * <li>The command to run.</li>
 * <li>Ports to expose.</li>
 * <li>Update and restart policies, etc.</li>
 * </ul>
 */
public interface Service
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the service's ID
	 * @return the type-safe identifier for the resource
	 * @throws NullPointerException     if {@code value} is null
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(String value)
	{
		return new Id(value);
	}

	/**
	 * Returns the service's ID.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * Returns the service's name.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Lists a service's tasks.
	 *
	 * @return the tasks
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	List<Task> listTasks() throws IOException, InterruptedException;

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value the service's ID
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
		 */
		private Id(String value)
		{
			super(value);
		}
	}
}