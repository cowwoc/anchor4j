package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.core.id.StringId;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * A snapshot of a node's state.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public interface Node
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the node's ID
	 * @return the type-safe identifier for the resource
	 * @throws NullPointerException     if {@code value} is null
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(String value)
	{
		return new Id(value);
	}

	/**
	 * Returns the node's id.
	 *
	 * @return the id
	 */
	Id getId();

	/**
	 * Returns the node's hostname.
	 *
	 * @return the hostname
	 */
	String getHostname();

	/**
	 * Returns the node's role.
	 *
	 * @return null if the node is not a member of a swarm
	 */
	Role getRole();

	/**
	 * Indicates if the node is a swarm leader.
	 *
	 * @return {@code true} if the node is a swarm leader
	 */
	boolean isLeader();

	/**
	 * Returns the status of the node.
	 *
	 * @return the status
	 */
	Status getStatus();

	/**
	 * Indicates whether it is possible to communicate with the node.
	 *
	 * @return {@link Reachability#UNKNOWN UNKNOWN} for worker nodes
	 */
	Reachability getReachability();

	/**
	 * Indicates if the node is available to run tasks.
	 *
	 * @return {@code true} if the node is available to run tasks
	 */
	Availability getAvailability();

	/**
	 * Returns the node's address for manager communication.
	 *
	 * @return an empty string for worker nodes
	 */
	String getManagerAddress();

	/**
	 * Returns the node's address.
	 *
	 * @return the address
	 */
	String getAddress();

	/**
	 * Returns values that are used to constrain task scheduling to specific nodes.
	 *
	 * @return values that are used to constrain task scheduling to specific nodes
	 */
	List<String> getLabels();

	/**
	 * Returns the docker version that the node is running.
	 *
	 * @return the docker version
	 */
	String getDockerVersion();

	/**
	 * Reloads the node's state.
	 *
	 * @return the updated state
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@CheckReturnValue
	Node reload() throws IOException, InterruptedException;

	/**
	 * Lists the tasks that are assigned to the node.
	 * <p>
	 * This includes tasks in active lifecycle states such as {@code New}, {@code Allocated}, {@code Pending},
	 * {@code Assigned}, {@code Accepted}, {@code Preparing}, {@code Ready}, {@code Starting}, and
	 * {@code Running}. These states represent tasks that are in progress or actively running and are reliably
	 * returned by this command.
	 * <p>
	 * However, tasks that have reached a terminal state—such as {@code Complete}, {@code Failed}, or
	 * {@code Shutdown}— are often pruned by Docker shortly after they exit, and are therefore not guaranteed to
	 * appear in the results, even if they completed very recently.
	 * <p>
	 * Note that Docker prunes old tasks aggressively from this command, so {@link Service#listTasks()} will
	 * often provide more comprehensive historical data by design.
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
	 * Begins gracefully removing tasks from this node and redistribute them to other active nodes.
	 *
	 * @return the ID of the updated node
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	Id drain() throws IOException, InterruptedException;

	/**
	 * Sets the role of a node.
	 *
	 * @param role     the new role
	 * @param deadline the absolute time by which the node's role must change. The method will poll the node's
	 *                 state while the current time is before this value.
	 * @return the ID of the updated node
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if a node attempts to modify its own role
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 * @throws TimeoutException         if the deadline expires before the operation succeeds
	 */
	Id setRole(Role role, Instant deadline) throws IOException, InterruptedException, TimeoutException;

	/**
	 * Removes a node from the swarm.
	 *
	 * @return an node remover
	 */
	@CheckReturnValue
	NodeRemover remove();

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value the node's ID
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
		 */
		private Id(String value)
		{
			super(value);
		}
	}

	/**
	 * Indicates if the node is available to run tasks.
	 */
	public enum Availability
	{
		// https://github.com/docker/engine-api/blob/4290f40c056686fcaa5c9caf02eac1dde9315adf/types/swarm/node.go#L34
		/**
		 * The node can accept new tasks.
		 */
		ACTIVE,
		/**
		 * The node is temporarily unavailable for new tasks, but existing tasks continue running.
		 */
		PAUSE,
		/**
		 * The node is unavailable for new tasks, and any existing tasks are being moved to other nodes in the
		 * swarm. This is typically used when preparing a node for maintenance.
		 */
		DRAIN;
	}

	/**
	 * Indicates if it is possible to communicate with the node.
	 */
	public enum Reachability
	{
		// https://github.com/docker/engine-api/blob/4290f40c056686fcaa5c9caf02eac1dde9315adf/types/swarm/node.go#L79
		/**
		 * There is insufficient information to determine if the node is reachable.
		 */
		UNKNOWN,
		/**
		 * The node is unreachable.
		 */
		UNREACHABLE,
		/**
		 * The node is reachable.
		 */
		REACHABLE;
	}

	/**
	 * Indicates the overall health of the node.
	 */
	public enum Status
	{
		// https://github.com/docker/engine-api/blob/4290f40c056686fcaa5c9caf02eac1dde9315adf/types/swarm/node.go#L98
		/**
		 * There is insufficient information to determine the status of the node.
		 */
		UNKNOWN,
		/**
		 * The node is permanently unreachable and unable to run tasks.
		 */
		DOWN,
		/**
		 * The node is reachable and ready to run tasks.
		 */
		READY,
		/**
		 * The node is temporarily unreachable but may still be running tasks.
		 */
		DISCONNECTED;
	}

	/**
	 * The role of the node within the swarm.
	 */
	public enum Role
	{
		/**
		 * A node that participates in administrating the swarm.
		 */
		MANAGER,
		/**
		 * A node that runs tasks.
		 */
		WORKER;
	}
}