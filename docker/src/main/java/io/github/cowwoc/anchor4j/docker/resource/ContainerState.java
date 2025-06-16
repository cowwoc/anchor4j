package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A snapshot of a container's state.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public final class ContainerState
{
	private final Docker client;
	private final String id;
	private final String name;
	private final HostConfiguration hostConfiguration;
	private final NetworkConfiguration networkConfiguration;
	private final Status status;

	/**
	 * Creates a state.
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
	public ContainerState(Docker client, String id, String name, HostConfiguration hostConfiguration,
		NetworkConfiguration networkConfiguration, Status status)
	{
		requireThat(client, "client").isNotNull();
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();
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

	/**
	 * Returns the ID of the container.
	 *
	 * @return the ID
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Returns the name of the container.
	 *
	 * @return an empty string if the container does not have a name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the container's host configuration.
	 *
	 * @return the host configuration
	 */
	public HostConfiguration getHostConfiguration()
	{
		return hostConfiguration;
	}

	/**
	 * Returns the container's network configuration.
	 *
	 * @return the network configuration
	 */
	public NetworkConfiguration getNetworkConfiguration()
	{
		return networkConfiguration;
	}

	/**
	 * Returns the container's status.
	 *
	 * @return the status
	 */
	public Status getStatus()
	{
		return status;
	}

	/**
	 * Reloads the container's state.
	 *
	 * @return the updated state
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@CheckReturnValue
	public ContainerState reload() throws IOException, InterruptedException
	{
		return client.getContainerState(id);
	}

	/**
	 * Starts this container.
	 *
	 * @return a container starter
	 */
	@CheckReturnValue
	public ContainerStarter starter()
	{
		return client.startContainer(id);
	}

	/**
	 * Stops this container.
	 *
	 * @return a container starter
	 */
	@CheckReturnValue
	public ContainerStopper stopper()
	{
		return client.stopContainer(id);
	}

	/**
	 * Removes this container.
	 *
	 * @return a container remover
	 */
	@CheckReturnValue
	public ContainerRemover remover()
	{
		return client.removeContainer(id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, hostConfiguration, networkConfiguration, status);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof ContainerState other && other.id.equals(id) && other.name.equals(name) &&
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

	/**
	 * Represents a port mapping entry for a Docker container.
	 *
	 * @param containerPort the container port number being exposed
	 * @param protocol      the transport protocol being exposed
	 * @param hostAddresses the host addresses to which the container port is bound
	 */
	public record PortBinding(int containerPort, Protocol protocol, List<InetSocketAddress> hostAddresses)
	{
		/**
		 * Creates a PortBinding.
		 *
		 * @param containerPort the container port number being exposed
		 * @param protocol      the transport protocol being exposed
		 * @param hostAddresses the host addresses to which the container port is bound
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code containerPort} is negative or zero
		 */
		public PortBinding(int containerPort, Protocol protocol, List<InetSocketAddress> hostAddresses)
		{
			requireThat(containerPort, "containerPort").isPositive();
			requireThat(protocol, "protocol").isNotNull();
			this.containerPort = containerPort;
			this.protocol = protocol;
			this.hostAddresses = List.copyOf(hostAddresses);
		}
	}

	/**
	 * A container's host configuration.
	 * <p>
	 * <b>Thread Safety</b>: This class is immutable and thread-safe.
	 *
	 * @param portBindings the bound ports
	 */
	public record HostConfiguration(List<PortBinding> portBindings)
	{
		/**
		 * Creates a configuration.
		 *
		 * @param portBindings the bound ports
		 * @throws NullPointerException if {@code portBindings} is null
		 */
		public HostConfiguration(List<PortBinding> portBindings)
		{
			this.portBindings = List.copyOf(portBindings);
		}
	}

	/**
	 * A container's network settings.
	 * <p>
	 * <b>Thread Safety</b>: This class is immutable and thread-safe.
	 *
	 * @param ports the bound ports
	 */
	public record NetworkConfiguration(List<PortBinding> ports)
	{
		/**
		 * Creates a configuration.
		 *
		 * @param ports the bound ports
		 * @throws NullPointerException if {@code ports} is null
		 */
		public NetworkConfiguration(List<PortBinding> ports)
		{
			this.ports = List.copyOf(ports);
		}
	}

	/**
	 * Represents the status of a container.
	 */
	public enum Status
	{
		/**
		 * The container was created but has never been started.
		 */
		CREATED,
		/**
		 * The container is running.
		 */
		RUNNING,
		/**
		 * The container is paused.
		 */
		PAUSED,
		/**
		 * The container is in the process of restarting.
		 */
		RESTARTING,
		/**
		 * A container which is no longer running.
		 */
		EXITED,
		/**
		 * A container which is in the process of being removed.
		 */
		REMOVING,
		/**
		 * The container was partially removed (e.g., because resources were kept busy by an external process). It
		 * cannot be (re)started, only removed.
		 */
		DEAD;
	}
}