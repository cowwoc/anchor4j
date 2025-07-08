package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.Service;
import io.github.cowwoc.anchor4j.docker.resource.Task;

import java.io.IOException;
import java.util.List;

public final class DefaultService implements Service
{
	private final InternalDockerClient client;
	private final Id id;

	/**
	 * Creates a reference to a service.
	 *
	 * @param client the client configuration
	 * @param id     the ID of the container
	 */
	public DefaultService(InternalDockerClient client, Id id)
	{
		assert client != null;
		assert id != null;
		this.client = client;
		this.id = id;
	}

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public List<Task> listTasks() throws IOException, InterruptedException
	{
		return client.listTasksByService(id);
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Service other && other.getId().equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DockerImage.class).
			add("id", id).
			toString();
	}
}