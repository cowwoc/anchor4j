package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Node;
import io.github.cowwoc.anchor4j.docker.resource.NodeRemover;
import io.github.cowwoc.anchor4j.docker.resource.Task;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultNode implements Node
{
	private final DockerClient client;
	private final Id id;
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
	 * Creates a DefaultNode.
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
	public DefaultNode(DockerClient client, Id id, String hostname, Role role, boolean leader,
		Status status, Reachability reachability, Availability availability, String managerAddress,
		String address, List<String> labels, String dockerVersion)
	{
		requireThat(client, "client").isNotNull();
		requireThat(id, "id").isNotNull();
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

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public String getHostname()
	{
		return hostname;
	}

	@Override
	public Role getRole()
	{
		return role;
	}

	@Override
	public boolean isLeader()
	{
		return leader;
	}

	@Override
	public Status getStatus()
	{
		return status;
	}

	@Override
	public Reachability getReachability()
	{
		return reachability;
	}

	@Override
	public Availability getAvailability()
	{
		return availability;
	}

	@Override
	public String getManagerAddress()
	{
		return managerAddress;
	}

	@Override
	public String getAddress()
	{
		return address;
	}

	@Override
	public List<String> getLabels()
	{
		return labels;
	}

	@Override
	public String getDockerVersion()
	{
		return dockerVersion;
	}

	@Override
	@CheckReturnValue
	public Node reload() throws IOException, InterruptedException
	{
		return client.getNode(id);
	}

	@Override
	public List<Task> listTasks() throws IOException, InterruptedException
	{
		return client.getTasksByNode();
	}

	@Override
	public Id drain() throws IOException, InterruptedException
	{
		return client.drainNode(id);
	}

	@Override
	public Id setRole(Role role, Instant deadline) throws IOException, InterruptedException, TimeoutException
	{
		return client.setNodeRole(id, role, deadline);
	}

	@Override
	public NodeRemover remove()
	{
		return client.removeNode(id);
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DefaultNode other && other.id.equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultNode.class).
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
}