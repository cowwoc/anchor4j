package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Context;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.Objects;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultContext implements Context
{
	private final DockerClient client;
	private final Id id;
	private final String description;
	private final String endpoint;

	/**
	 * Creates a DefaultContext.
	 *
	 * @param client      the client configuration
	 * @param id          the context's ID
	 * @param description the context's description
	 * @param endpoint    the configuration of the target Docker Engine
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>any of the arguments contain whitespace.</li>
	 *                                    <li>{@code name} or {@code endpoint} are empty.</li>
	 *                                  </ul>
	 */
	public DefaultContext(DockerClient client, Id id, String description, String endpoint)
	{
		requireThat(client, "client").isNotNull();
		requireThat(id, "id").isNotNull();
		requireThat(description, "description").doesNotContainWhitespace();
		requireThat(endpoint, "endpoint").doesNotContainWhitespace().isNotEmpty();

		this.client = client;
		this.id = id;
		this.description = description;
		this.endpoint = endpoint;
	}

	@Override
	public String getName()
	{
		return id.getValue();
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	@CheckReturnValue
	public Context reload() throws IOException, InterruptedException
	{
		return client.getContext(id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, description, endpoint);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DefaultContext other && other.id.equals(id) &&
			other.description.equals(description) && other.endpoint.equals(endpoint);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultContext.class).
			add("id", id).
			add("description", description).
			add("endpoint", endpoint).
			toString();
	}
}