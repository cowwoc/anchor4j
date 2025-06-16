package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.Docker;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * An element returned by {@link Docker#listContainers()}.
 *
 * @param container the container
 * @param name      the container's name
 */
public record ContainerElement(Container container, String name)
{
	/**
	 * Creates a container element.
	 *
	 * @param container the container
	 * @param name      the container's name
	 */
	public ContainerElement
	{
		assert container != null;
		assert that(name, "name").doesNotContainWhitespace().isNotEmpty().elseThrow();
	}
}