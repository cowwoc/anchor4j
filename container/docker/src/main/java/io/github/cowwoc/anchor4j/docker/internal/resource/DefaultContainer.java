package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Container;
import io.github.cowwoc.anchor4j.docker.resource.ContainerLogs;
import io.github.cowwoc.anchor4j.docker.resource.ContainerRemover;
import io.github.cowwoc.anchor4j.docker.resource.ContainerStarter;
import io.github.cowwoc.anchor4j.docker.resource.ContainerStopper;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.Objects;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultContainer implements Container
{
	private final DockerClient client;
	private final Id id;
	private final String name;
	private final HostConfiguration hostConfiguration;
	private final NetworkConfiguration networkConfiguration;
	private final Status status;

	/**
	 * Creates a DefaultContainer.
	 *
	 * @param client               the client configuration
	 * @param id                   the ID of the container
	 * @param name                 the name of the container, or an empty string if the container does not have
	 *                             a name
	 * @param hostConfiguration    the container's host configuration
	 * @param networkConfiguration the container's network configuration
	 * @param status               the container's status
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code id} or {@code name} contain whitespace or are empty
	 */
	public DefaultContainer(DockerClient client, Id id, String name, HostConfiguration hostConfiguration,
		NetworkConfiguration networkConfiguration, Status status)
	{
		requireThat(client, "client").isNotNull();
		requireThat(id, "id").isNotNull();
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();
		requireThat(hostConfiguration, "hostConfiguration").isNotNull();
		requireThat(status, "status").isNotNull();
		this.client = client;
		this.id = id;
		this.name = name;
		this.hostConfiguration = hostConfiguration;
		this.networkConfiguration = networkConfiguration;
		this.status = status;
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
	public HostConfiguration getHostConfiguration()
	{
		return hostConfiguration;
	}

	@Override
	public NetworkConfiguration getNetworkConfiguration()
	{
		return networkConfiguration;
	}

	@Override
	public Status getStatus()
	{
		return status;
	}

	@Override
	public Container rename(String newName) throws IOException, InterruptedException
	{
		client.renameContainer(id, newName);
		return this;
	}

	@Override
	@CheckReturnValue
	public Container reload() throws IOException, InterruptedException
	{
		return client.getContainer(id);
	}

	@Override
	@CheckReturnValue
	public ContainerStarter start()
	{
		return client.startContainer(id);
	}

	@Override
	@CheckReturnValue
	public ContainerStopper stop()
	{
		return client.stopContainer(id);
	}

	@Override
	@CheckReturnValue
	public ContainerRemover remove()
	{
		return client.removeContainer(id);
	}

	@Override
	public int waitUntilStop() throws IOException, InterruptedException
	{
		return client.waitUntilContainerStops(id);
	}

	@Override
	public Container waitUntilStatus(Status status) throws IOException, InterruptedException
	{
		return client.waitUntilContainerStatus(status, id);
	}

	@Override
	public ContainerLogs getLogs()
	{
		return client.getContainerLogs(id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, hostConfiguration, networkConfiguration, status);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DefaultContainer other && other.id.equals(id) && other.name.equals(name) &&
			other.hostConfiguration.equals(hostConfiguration) &&
			other.networkConfiguration.equals(networkConfiguration);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder().
			add("id", id).
			add("name", name).
			add("hostConfiguration", hostConfiguration).
			add("networkConfiguration", networkConfiguration).
			add("status", status).
			toString();
	}
}