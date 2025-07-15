package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Network.Id;

import java.util.function.Predicate;

/**
 * The properties used by the predicate in {@link DockerClient#getNetworks(Predicate)}.
 *
 * @param id   the network's ID
 * @param name the network's name
 */
public record NetworkElement(Id id, String name)
{
	/**
	 * Creates an image element.
	 *
	 * @param id   the network's ID
	 * @param name the network's name
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code name}'s format is invalid
	 */
	public NetworkElement
	{
		assert id != null;
		ParameterValidator.validateName(name, "name");
	}
}