package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Service.Id;

import java.util.function.Predicate;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * The properties used by the predicate in {@link DockerClient#getServices(Predicate)}.
 *
 * @param id   the node's ID
 * @param name the name of the service
 */
public record ServiceElement(Id id, String name)
{
	/**
	 * Creates a node element.
	 *
	 * @param id   the node's ID
	 * @param name the name of the service
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code name}'s format is invalid
	 */
	public ServiceElement
	{
		assert id != null;
		assert that(name, "name").doesNotContainWhitespace().isNotEmpty().elseThrow();
	}
}