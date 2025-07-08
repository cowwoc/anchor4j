package io.github.cowwoc.anchor4j.container.core.internal.resource;

import io.github.cowwoc.anchor4j.container.core.client.ContainerClient;
import io.github.cowwoc.anchor4j.container.core.resource.Builder;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.Objects;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultBuilder implements Builder
{
	private final ContainerClient client;
	private final Id id;
	private final Status status;
	private final String error;

	/**
	 * Creates a BuilderState.
	 *
	 * @param client the client configuration
	 * @param id     builder's ID
	 * @param status the builder's status
	 * @param error  an explanation of the builder's error status, or an empty string if the status is not
	 *               {@code ERROR}
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code name}'s format is invalid
	 */
	public DefaultBuilder(ContainerClient client, Id id, Status status, String error)
	{
		requireThat(client, "client").isNotNull();
		requireThat(status, "status").isNotNull();
		requireThat(error, "error").isNotNull();

		this.client = client;
		this.id = id;
		this.status = status;
		this.error = error;
	}

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public String getName()
	{
		return id.getValue();
	}

	@Override
	public Status getStatus()
	{
		return status;
	}

	@Override
	public String getError()
	{
		return error;
	}

	@Override
	@CheckReturnValue
	public Builder reload() throws IOException, InterruptedException
	{
		return client.getBuilder(id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, status, error);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Builder other && other.getId().equals(id) && other.getStatus().equals(status) &&
			other.getError().equals(error);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder().
			add("id", id).
			add("status", status).
			add("error", error).
			toString();
	}
}