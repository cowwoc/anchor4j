package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A snapshot of a node's state.
 * <p>
 * <b>Thread Safety</b>: This class is immutable and thread-safe.
 */
public final class NodeState
{
	private final Docker client;
	private final String id;
	private final String hostname;
	private final Role role;
	private final boolean leader;
	private final Availability availability;
	private final Reachability reachability;
	private final Status status;
	private final String managerAddress;
	private final String address;
	private final List<String> labels;
	private final String dockerVersion;

	/**
	 * Creates a DefaultNodeState.
	 *
	 * @param client         the client configuration
	 * @param id             the node's ID
	 * @param hostname       the node's hostname
	 * @param role           the role of the node
	 * @param leader         {@code true} if the node is a swarm leader
	 * @param status         the status of the node
	 * @param reachability   indicates if the node is reachable ({@link Reachability#UNKNOWN UNKNOWN} for worker
	 *                       nodes)
	 * @param availability   indicates if the node is available to run tasks
	 * @param managerAddress the node's address for manager communication, or an empty string for worker nodes
	 * @param address        the node's address
	 * @param labels         values that are used to constrain task scheduling to specific nodes
	 * @param dockerVersion  the docker version that the node is running
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code id}, {@code hostname}, {@code address} or
	 *                                    {@code dockerVersion} contain whitespace or are empty.</li>
	 *                                    <li>{@code role == Role.MANAGER} and {@code managerAddress}
	 *                                    contains whitespace or is empty.</li>
	 *                                  </ul>
	 */
	public NodeState(Docker client, String id, String hostname, Role role, boolean leader,
		Status status, Reachability reachability, Availability availability, String managerAddress,
		String address, List<String> labels, String dockerVersion)
	{
		requireThat(client, "client").isNotNull();
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();
		requireThat(hostname, "hostname").doesNotContainWhitespace().isNotEmpty();
		requireThat(role, "role").isNotNull();
		requireThat(status, "status").isNotNull();
		requireThat(reachability, "reachability").isNotNull();
		requireThat(availability, "availability").isNotNull();
		requireThat(address, "address").doesNotContainWhitespace().isNotEmpty();
		requireThat(dockerVersion, "dockerVersion").doesNotContainWhitespace().isNotEmpty();

		if (role == Role.MANAGER)
			requireThat(managerAddress, "managerAddress").doesNotContainWhitespace().isNotEmpty();
		requireThat(labels, "labels").isNotNull();
		this.client = client;
		this.id = id;
		this.hostname = hostname;
		this.role = role;
		this.leader = leader;
		this.status = status;
		this.reachability = reachability;
		this.availability = availability;
		this.managerAddress = managerAddress;
		this.address = address;
		this.labels = List.copyOf(labels);
		this.dockerVersion = dockerVersion;
	}

	/**
	 * Returns the node's id.
	 *
	 * @return the id
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Returns the node's hostname.
	 *
	 * @return the hostname
	 */
	public String getHostname()
	{
		return hostname;
	}

	/**
	 * Returns the node's role.
	 *
	 * @return null if the node is not a member of a swarm
	 */
	public Role getRole()
	{
		return role;
	}

	/**
	 * Indicates if the node is a swarm leader.
	 *
	 * @return {@code true} if the node is a swarm leader
	 */
	public boolean isLeader()
	{
		return leader;
	}

	/**
	 * Returns the status of the node.
	 *
	 * @return the status
	 */
	public Status getStatus()
	{
		return status;
	}

	/**
	 * Indicates whether it is possible to communicate with the node.
	 *
	 * @return {@link Reachability#UNKNOWN UNKNOWN} for worker nodes
	 */
	public Reachability getReachability()
	{
		return reachability;
	}

	/**
	 * Indicates if the node is available to run tasks.
	 *
	 * @return {@code true} if the node is available to run tasks
	 */
	public Availability getAvailability()
	{
		return availability;
	}

	/**
	 * Returns the node's address for manager communication.
	 *
	 * @return an empty string for worker nodes
	 */
	public String getManagerAddress()
	{
		return managerAddress;
	}

	/**
	 * Returns the node's address.
	 *
	 * @return the address
	 */
	public String getAddress()
	{
		return address;
	}

	/**
	 * Returns values that are used to constrain task scheduling to specific nodes.
	 *
	 * @return values that are used to constrain task scheduling to specific nodes
	 */
	public List<String> getLabels()
	{
		return labels;
	}

	/**
	 * Returns the docker version that the node is running.
	 *
	 * @return the docker version
	 */
	public String getDockerVersion()
	{
		return dockerVersion;
	}

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
	public NodeState reload() throws IOException, InterruptedException
	{
		return client.getNodeState(id);
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof NodeState other && other.id.equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(NodeState.class).
			add("id", id).
			add("role", role).
			add("leader", leader).
			add("availability", availability).
			add("reachability", reachability).
			add("status", status).
			add("managerAddress", managerAddress).
			add("workerAddress", address).
			add("hostname", hostname).
			add("labels", labels).
			add("engineVersion", dockerVersion).
			toString();
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
		 * The node is permanently unable to run tasks.
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