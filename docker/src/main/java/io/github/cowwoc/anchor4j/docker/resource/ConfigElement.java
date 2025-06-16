package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.Docker;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * An element returned by {@link Docker#listConfigs()}.
 *
 * @param id   the config's ID
 * @param name the config's name
 */
public record ConfigElement(String id, String name)
{
	/**
	 * Creates an element.
	 *
	 * @param id   the config's ID
	 * @param name the config's name
	 */
	public ConfigElement
	{
		assert that(id, "id").doesNotContainWhitespace().isNotEmpty().elseThrow();
		assert that(name, "name").doesNotContainWhitespace().isNotEmpty().elseThrow();
	}
}