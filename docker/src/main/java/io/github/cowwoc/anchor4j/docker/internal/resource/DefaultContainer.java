package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.Container;
import io.github.cowwoc.anchor4j.docker.resource.ContainerLogs;
import io.github.cowwoc.anchor4j.docker.resource.ContainerRemover;
import io.github.cowwoc.anchor4j.docker.resource.ContainerStarter;
import io.github.cowwoc.anchor4j.docker.resource.ContainerState;
import io.github.cowwoc.anchor4j.docker.resource.ContainerStopper;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;

import java.io.IOException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * The default implementation of {@code Container}.
 */
public final class DefaultContainer implements Container
{
	private final InternalDocker client;
	private final String id;

	/**
	 * Creates a reference to a container.
	 *
	 * @param client the client configuration
	 * @param id     the ID of the container
	 */
	public DefaultContainer(InternalDocker client, String id)
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
	public ContainerState getState() throws IOException, InterruptedException
	{
		return client.getContainerState(id);
	}

	@Override
	public Container rename(String newName) throws IOException, InterruptedException
	{
		client.renameContainer(id, newName);
		return this;
	}

	@Override
	public ContainerStarter starter()
	{
		return client.startContainer(id);
	}

	@Override
	public ContainerStopper stopper()
	{
		return client.stopContainer(id);
	}

	@Override
	public ContainerRemover remover()
	{
		return client.removeContainer(id);
	}

	@Override
	public int waitUntilStop() throws IOException, InterruptedException
	{
		return client.waitUntilContainerStops(id);
	}

	@Override
	public ContainerLogs getLogs()
	{
		return client.getContainerLogs(id);
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DefaultContainer other && other.id.equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DockerImage.class).
			add("id", id).
			toString();
	}
}