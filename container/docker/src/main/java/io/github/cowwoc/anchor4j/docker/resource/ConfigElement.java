package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Config.Id;

import java.util.function.Predicate;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * The properties used by the predicate in {@link DockerClient#getConfigs(Predicate)}.
 *
 * @param id   the config's ID
 * @param name the config's name
 */
public record ConfigElement(Id id, String name)
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