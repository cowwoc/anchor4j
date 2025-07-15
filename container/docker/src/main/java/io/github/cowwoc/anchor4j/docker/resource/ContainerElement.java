package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Container.Id;

import java.util.function.Predicate;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * The properties used by the predicate in {@link DockerClient#getContainers(Predicate)}.
 *
 * @param id   the container's ID
 * @param name the container's name
 */
public record ContainerElement(Id id, String name)
{
	/**
	 * Creates a container element.
	 *
	 * @param id   the container's ID
	 * @param name the container's name
	 */
	public ContainerElement
	{
		assert id != null;
		assert that(name, "name").doesNotContainWhitespace().isNotEmpty().elseThrow();
	}
}