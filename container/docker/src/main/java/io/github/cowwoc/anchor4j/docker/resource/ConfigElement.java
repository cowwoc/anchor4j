package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.DockerClient;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * An element returned by {@link DockerClient#listConfigs()}.
 *
 * @param id   the config's ID
 * @param name the config's name
 */
public record ConfigElement(Config.Id id, String name)
{
	/**
	 * Creates an element.
	 *
	 * @param id   the config's ID
	 * @param name the config's name
	 */
	public ConfigElement
	{
		assert id != null;
		assert that(name, "name").doesNotContainWhitespace().isNotEmpty().elseThrow();
	}
}