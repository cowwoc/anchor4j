package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.id.StringId;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A docker container, which is a running instance of an image.
 * <p>
 * <b>Thread Safety</b>: This class is immutable and thread-safe.
 */
public interface Container
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the container's ID or name
	 * @return the type-safe identifier for the resource
	 * @throws NullPointerException     if {@code value} is null
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(String value)
	{
		return new Id(value);
	}

	/**
	 * Returns the ID of the container.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * Returns the name of the container.
	 *
	 * @return an empty string if the container does not have a name
	 */
	String getName();

	/**
	 * Returns the container's host configuration.
	 *
	 * @return the host configuration
	 */
	HostConfiguration getHostConfiguration();

	/**
	 * Returns the container's network configuration.
	 *
	 * @return the network configuration
	 */
	NetworkConfiguration getNetworkConfiguration();

	/**
	 * Returns the container's status.
	 *
	 * @return the status
	 */
	Status getStatus();

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
	Container reload() throws IOException, InterruptedException;

	/**
	 * Renames the container.
	 *
	 * @param newName the container's new name
	 * @return this
	 * @throws NullPointerException      if {@code newName} is null
	 * @throws IllegalArgumentException  if {@code newName}:
	 *                                   <ul>
	 *                                     <li>is empty.</li>
	 *                                     <li>contains any character other than lowercase letters (a–z),
	 *                                     digits (0–9), and the following characters: {@code '.'}, {@code '/'},
	 *                                     {@code ':'}, {@code '_'}, {@code '-'}, {@code '@'}.</li>
	 *                                   </ul>
	 * @throws ResourceNotFoundException if the container does not exist
	 * @throws ResourceInUseException    if the requested name is in use by another container
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	Container rename(String newName) throws IOException, InterruptedException;

	/**
	 * Starts the container.
	 *
	 * @return a container starter
	 */
	@CheckReturnValue
	ContainerStarter start();

	/**
	 * Stops the container.
	 *
	 * @return a container stopper
	 */
	@CheckReturnValue
	ContainerStopper stop();

	/**
	 * Removes the container.
	 *
	 * @return a container remover
	 */
	@CheckReturnValue
	ContainerRemover remove();

	/**
	 * Waits until the container stops.
	 * <p>
	 * If the container has already stopped, this method returns immediately.
	 *
	 * @return the exit code returned by the container
	 * @throws ResourceNotFoundException if the container does not exist
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	int waitUntilStop() throws IOException, InterruptedException;

	/**
	 * Waits until the container reaches the desired status.
	 * <p>
	 * If the container already has the desired status, this method returns immediately.
	 *
	 * @param status the desired status
	 * @return the updated container
	 * @throws ResourceNotFoundException if the container does not exist
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	Container waitUntilStatus(Status status) throws IOException, InterruptedException;

	/**
	 * Retrieves the container's logs.
	 *
	 * @return the logs
	 */
	ContainerLogs getLogs();

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value the container's ID or name
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value}'s format is invalid
		 */
		private Id(String value)
		{
			super(value);
			ParameterValidator.validateContainerIdOrName(value, "value");
		}
	}

	/**
	 * Represents a port mapping entry for a Docker container.
	 *
	 * @param containerPort the container port number being exposed
	 * @param protocol      the transport protocol being exposed
	 * @param hostAddresses the host addresses to which the container port is bound
	 */
	record PortBinding(int containerPort, Protocol protocol, List<InetSocketAddress> hostAddresses)
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
	record HostConfiguration(List<PortBinding> portBindings)
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
	record NetworkConfiguration(List<PortBinding> ports)
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
	enum Status
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
		DEAD
	}
}