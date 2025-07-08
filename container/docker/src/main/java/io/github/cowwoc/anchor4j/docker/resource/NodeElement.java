package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Node.Availability;
import io.github.cowwoc.anchor4j.docker.resource.Node.Id;
import io.github.cowwoc.anchor4j.docker.resource.Node.Reachability;
import io.github.cowwoc.anchor4j.docker.resource.Node.Role;
import io.github.cowwoc.anchor4j.docker.resource.Node.Status;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * An element returned by {@link DockerClient#listNodes()}.
 *
 * @param id            the node's ID
 * @param hostname      the node's hostname
 * @param role          the type of the node
 * @param leader        {@code true} if the node is a swarm leader
 * @param status        the status of the node
 * @param reachability  indicates if the node is reachable ({@link Reachability#UNKNOWN UNKNOWN} for worker
 *                      nodes)
 * @param availability  indicates if the node is available to run tasks
 * @param dockerVersion the version of docker engine that the node is running
 */
public record NodeElement(Id id, String hostname, Role role, boolean leader, Status status,
                          Reachability reachability, Availability availability, String dockerVersion)
{
	/**
	 * Creates a node element.
	 *
	 * @param id            the node's ID
	 * @param hostname      the node's hostname
	 * @param role          the type of the node
	 * @param leader        {@code true} if the node is a swarm leader
	 * @param status        the status of the node
	 * @param reachability  indicates if the node is reachable ({@link Reachability#UNKNOWN UNKNOWN} for worker
	 *                      nodes)
	 * @param availability  indicates if the node is available to run tasks
	 * @param dockerVersion the Docker version that the node is running
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code hostName} or {@code dockerVersion} contain whitespace or are
	 *                                  empty
	 */
	public NodeElement
	{
		requireThat(id, "id").isNotNull();
		requireThat(hostname, "hostname").doesNotContainWhitespace().isNotEmpty();
		requireThat(role, "role").isNotNull();
		requireThat(status, "status").isNotNull();
		requireThat(reachability, "reachability").isNotNull();
		requireThat(availability, "availability").isNotNull();
		requireThat(dockerVersion, "dockerVersion").doesNotContainWhitespace();
	}
}