package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.Service;
import io.github.cowwoc.anchor4j.docker.resource.TaskState;

import java.io.IOException;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * The default implementation of a {@code Service}.
 */
public final class DefaultService implements Service
{
	private final InternalDocker client;
	private final String id;

	/**
	 * Creates a reference to a service.
	 *
	 * @param client the client configuration
	 * @param id     the ID of the container
	 */
	public DefaultService(InternalDocker client, String id)
	{
		assert client != null;
		assert that(id, "id").doesNotContainWhitespace().isNotEmpty().elseThrow();
		this.client = client;
		this.id = id;
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public List<TaskState> listTasks() throws IOException, InterruptedException
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
		return o instanceof DefaultService other && other.id.equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DockerImage.class).
			add("id", id).
			toString();
	}
}