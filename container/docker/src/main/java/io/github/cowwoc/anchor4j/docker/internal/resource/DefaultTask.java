package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Task;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.Objects;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultTask implements Task
{
	private final DockerClient client;
	private final Id id;
	private final String name;
	private final State state;

	/**
	 * Creates a reference to a task.
	 *
	 * @param client the client configuration
	 * @param id     the task's ID
	 * @param name   the task's name
	 * @param state  the task's state
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if any of the arguments contain whitespace or are empty
	 */
	public DefaultTask(DockerClient client, Id id, String name, State state)
	{
		requireThat(client, "client").isNotNull();
		requireThat(id, "id").isNotNull();
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();
		requireThat(state, "state").isNotNull();
		this.client = client;
		this.id = id;
		this.name = name;
		this.state = state;
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
	public State getState()
	{
		return state;
	}

	@Override
	@CheckReturnValue
	public Task reload() throws IOException, InterruptedException
	{
		return client.getTask(id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, state);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DefaultTask other && other.id.equals(id) && other.name.equals(name) &&
			other.state.equals(state);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultTask.class).
			add("id", id).
			add("name", name).
			add("state", state).
			toString();
	}
}