package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.DockerClient;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * An element returned by {@link DockerClient#listContainers()}.
 *
 * @param id   the container's ID
 * @param name the container's name
 */
public record ContainerElement(Container.Id id, String name)
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