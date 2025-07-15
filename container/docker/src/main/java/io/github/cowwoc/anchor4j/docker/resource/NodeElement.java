package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Node.Availability;
import io.github.cowwoc.anchor4j.docker.resource.Node.Id;
import io.github.cowwoc.anchor4j.docker.resource.Node.Reachability;
import io.github.cowwoc.anchor4j.docker.resource.Node.Role;
import io.github.cowwoc.anchor4j.docker.resource.Node.Status;

import java.util.function.Predicate;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * The properties used by the predicate in {@link DockerClient#getNodes(Predicate)}.
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
		assert id != null;
		assert that(hostname, "hostname").doesNotContainWhitespace().isNotEmpty().elseThrow();
		assert that(role, "role").isNotNull().elseThrow();
		assert that(status, "status").isNotNull().elseThrow();
		assert that(reachability, "reachability").isNotNull().elseThrow();
		assert that(availability, "availability").isNotNull().elseThrow();
		assert that(dockerVersion, "dockerVersion").doesNotContainWhitespace().elseThrow();
	}
}