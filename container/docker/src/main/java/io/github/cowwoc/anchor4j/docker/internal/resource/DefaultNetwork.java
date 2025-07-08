package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Network;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultNetwork implements Network
{
	private final DockerClient client;
	private final Id id;
	private final String name;
	private final List<Configuration> configurations;

	/**
	 * Creates a DefaultNetwork.
	 *
	 * @param client         the client configuration
	 * @param id             the ID of the network
	 * @param name           the name of the network
	 * @param configurations the network configurations
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code id} or {@code name} contain whitespace or are empty
	 */
	public DefaultNetwork(DockerClient client, Id id, String name, List<Configuration> configurations)
	{
		requireThat(client, "client").isNotNull();
		requireThat(id, "id").isNotNull();
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();

		this.client = client;
		this.id = id;
		this.name = name;
		this.configurations = List.copyOf(configurations);
	}

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public List<Configuration> getConfigurations()
	{
		return configurations;
	}

	@Override
	@CheckReturnValue
	public Network reload() throws IOException, InterruptedException
	{
		return client.getNetwork(id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, configurations);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof DefaultNetwork other && other.id.equals(id) && other.name.equals(name) &&
			other.configurations.equals(configurations);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder().
			add("id", id).
			add("name", name).
			add("configurations", configurations).
			toString();
	}
}