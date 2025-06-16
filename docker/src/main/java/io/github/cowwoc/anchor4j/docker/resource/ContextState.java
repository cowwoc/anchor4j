package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.Objects;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * The state of a Docker context.
 */
public final class ContextState
{
	private final Docker client;
	private final String name;
	private final String description;
	private final String endpoint;

	/**
	 * Creates a state.
	 *
	 * @param client      the client configuration
	 * @param name        the context's name
	 * @param description the context's description
	 * @param endpoint    the configuration of the target Docker Engine
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>any of the arguments contain whitespace.</li>
	 *                                    <li>{@code name} or {@code endpoint} are empty.</li>
	 *                                  </ul>
	 */
	public ContextState(Docker client, String name, String description, String endpoint)
	{
		requireThat(client, "client").isNotNull();
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();
		requireThat(description, "description").doesNotContainWhitespace();
		requireThat(endpoint, "endpoint").doesNotContainWhitespace().isNotEmpty();

		this.client = client;
		this.name = name;
		this.description = description;
		this.endpoint = endpoint;
	}

	/**
	 * Returns the context's name.
	 *
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the context's description.
	 *
	 * @return an empty string if omitted
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * Reloads the context's state.
	 *
	 * @return the updated state
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@CheckReturnValue
	public ContextState reload() throws IOException, InterruptedException
	{
		return client.getContextState(name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, description, endpoint);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof ContextState other && other.name.equals(name) &&
			other.description.equals(description) && other.endpoint.equals(endpoint);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(ContextState.class).
			add("name", name).
			add("description", description).
			add("endpoint", endpoint).
			toString();
	}
}