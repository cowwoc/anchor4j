package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static io.github.cowwoc.anchor4j.docker.resource.Protocol.TCP;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Creates a container.
 */
public interface ContainerCreator
{
	/**
	 * Sets the name of the container.
	 *
	 * @param name the container name. The value must start with a letter, or digit, or underscore, and may be
	 *             followed by additional characters consisting of letters, digits, underscores, periods or
	 *             hyphens.
	 * @return this
	 * @throws NullPointerException     if {@code name} is null
	 * @throws IllegalArgumentException if {@code name}'s format is invalid
	 */
	ContainerCreator name(String name);

	/**
	 * Sets the platform of the container.
	 *
	 * @param platform the platform
	 * @return this
	 * @throws NullPointerException     if {@code platform} is null
	 * @throws IllegalArgumentException if {@code platform} contains whitespace or is empty
	 */
	ContainerCreator platform(String platform);

	/**
	 * Sets the container's {@code ENTRYPOINT}, overriding any value set in the Dockerfile. If a {@code CMD}
	 * instruction is present in the Dockerfile, it will be ignored when {@code ENTRYPOINT} is overridden.
	 *
	 * @param entrypoint the command
	 * @return this
	 * @throws NullPointerException     if {@code command} is null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code command} is empty.</li>
	 *                                    <li>any of {@code command}'s elements contain whitespace or are
	 *                                    empty.</li>
	 *                                  </ul>
	 */
	ContainerCreator entrypoint(String... entrypoint);

	/**
	 * Sets the container's {@code ENTRYPOINT}, overriding any value set in the Dockerfile. If a {@code CMD}
	 * instruction is present in the Dockerfile, it will be ignored when {@code ENTRYPOINT} is overridden.
	 *
	 * @param entrypoint the command
	 * @return this
	 * @throws NullPointerException     if {@code command} is null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code command} is empty.</li>
	 *                                    <li>any of {@code command}'s elements contain whitespace or are
	 *                                    empty.</li>
	 *                                  </ul>
	 */
	ContainerCreator entrypoint(List<String> entrypoint);

	/**
	 * Sets the container's {@code CMD}, overriding any value set in the Dockerfile or becoming the full command
	 * if the Dockerfile does not contain an {@code ENTRYPOINT}, and no new {@link #entrypoint(List)} is
	 * specified.
	 *
	 * @param arguments the arguments
	 * @return this
	 * @throws NullPointerException     if {@code arguments} is null
	 * @throws IllegalArgumentException if {@code arguments} contains whitespace or is empty
	 */
	ContainerCreator arguments(String... arguments);

	/**
	 * Sets the container's {@code CMD}, overriding any value set in the Dockerfile or becoming the full command
	 * if the Dockerfile does not contain an {@code ENTRYPOINT}, and no new {@link #entrypoint(List)} is
	 * specified.
	 *
	 * @param arguments the arguments
	 * @return this
	 * @throws NullPointerException     if {@code arguments} is null
	 * @throws IllegalArgumentException if {@code arguments} contains whitespace or is empty
	 */
	ContainerCreator arguments(List<String> arguments);

	/**
	 * Sets the working directory to run commands in.
	 *
	 * @param workingDirectory the working directory
	 * @return this
	 * @throws NullPointerException     if {@code workingDirectory} is null
	 * @throws IllegalArgumentException if {@code workingDirectory} contains whitespace or is empty
	 */
	ContainerCreator workingDirectory(String workingDirectory);

	/**
	 * Adds or replaces an environment variable.
	 *
	 * @param name  the name of the variable
	 * @param value the value of the variable
	 * @return this
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if any of the arguments contain whitespace
	 */
	ContainerCreator environmentVariable(String name, String value);

	/**
	 * Binds a path from the host to the container. Any modification applied on either end is mirrored to the
	 * other.
	 *
	 * @param hostPath      a path on the host
	 * @param containerPath a path on the container
	 * @param options       mounting options
	 * @return this
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code hostPath} or {@code containerPath} contain leading or
	 *                                    trailing whitespace or are empty.</li>
	 *                                    <li>{@code containerPath} is not an absolute path.</li>
	 *                                  </ul>
	 */
	ContainerCreator bindPath(Path hostPath, String containerPath, BindMountOptions... options);

	/**
	 * Binds a container port to a host port.
	 *
	 * @param portBinding the port configuration
	 * @return this
	 * @throws NullPointerException if {@code portBinding} is null
	 */
	ContainerCreator bindPort(PortBinding portBinding);

	/**
	 * Indicates that the container and its associated anonymous volumes should be automatically removed upon
	 * exit.
	 *
	 * @return this
	 * @throws IllegalArgumentException if {@code restartPolicy} is enabled
	 */
	ContainerCreator removeOnExit();

	/**
	 * Indicates that the container should automatically restart when it stops, regardless of the reason. By
	 * default, the container is not restarted automatically.
	 *
	 * @return this
	 * @throws IllegalArgumentException {@code removeOnExit} is {@code true}
	 */
	ContainerCreator alwaysRestart();

	/**
	 * Indicates that the container should automatically restart unless it is manually stopped. By default, the
	 * container is not restarted automatically.
	 *
	 * @return this
	 * @throws IllegalArgumentException {@code removeOnExit} is {@code true}
	 */
	ContainerCreator restartUnlessStopped();

	/**
	 * Configures the container to restart if its exit code is non-zero. By default, the container is not
	 * restarted automatically.
	 *
	 * @param maximumAttempts the number of times to retry before giving up
	 * @return this
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code removeOnExit} is {@code true}.</li>
	 *                                    <li>{@code maximumAttempts} is negative or zero.</li>
	 *                                  </ul>
	 */
	ContainerCreator restartOnFailure(int maximumAttempts);

	/**
	 * Grants the container permission to do almost everything that the host can do. This is typically used to
	 * run Docker inside Docker.
	 * <ul>
	 * <li>Enables all Linux kernel capabilities.</li>
	 * <li>Disables the default seccomp profile.</li>
	 * <li>Disables the default AppArmor profile.</li>
	 * <li>Disables the SELinux process label.</li>
	 * <li>Makes {@code /sys} read-write Makes.</li>
	 * <li>Makes {@code cgroups} mounts read-write.</li>
	 * </ul>
	 * Use this flag with caution. Containers in this mode can get a root shell on the host and take control
	 * over the system.
	 *
	 * @return this
	 */
	ContainerCreator privileged();

	/**
	 * Creates the container.
	 *
	 * @return the new container
	 * @throws ResourceNotFoundException if the referenced image is not available locally and cannot be pulled
	 *                                   from Docker Hub, either because the repository does not exist or
	 *                                   requires different authentication credentials
	 * @throws ResourceInUseException    if the requested name is in use by another container
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	Container apply() throws IOException, InterruptedException;

	/**
	 * Options that apply to bind mounts.
	 *
	 * @see <a href="https://docs.docker.com/engine/storage/bind-mounts/">bind mounts</a>
	 */
	enum BindMountOptions
	{
		/**
		 * Prevents the container from modifying files inside the bind mount.
		 */
		READ_ONLY;

		/**
		 * Returns the version's JSON representation.
		 *
		 * @return the JSON representation
		 */
		public String toJson()
		{
			return switch (this)
			{
				case READ_ONLY -> "readonly";
			};
		}
	}

	/**
	 * The configuration of a bind mount.
	 *
	 * @param containerPath a path on the container
	 * @param options       mounting options
	 * @see <a href="https://docs.docker.com/engine/storage/bind-mounts/">bind mounts</a>
	 */
	record BindMount(String containerPath, BindMountOptions... options)
	{
		/**
		 * Creates a mount.
		 *
		 * @param containerPath a path on the container
		 * @param options       mounting options
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code containerPath} is not an absolute path
		 */
		public BindMount(String containerPath, BindMountOptions... options)
		{
			requireThat(containerPath, "containerPath").startsWith("/");
			for (BindMountOptions option : options)
				requireThat(option, "option").isNotNull();
			this.containerPath = containerPath;
			this.options = Arrays.copyOf(options, options.length);
		}
	}

	/**
	 * A builder for configuring and creating a {@link PortBinding}, which maps a container port to a specific
	 * host address and port.
	 */
	final class PortBindingBuilder
	{
		private final int containerPort;
		private Protocol protocol = TCP;
		private InetAddress hostAddress;
		private int hostPort;

		/**
		 * Creates a binding.
		 *
		 * @param containerPort the container port to bind to
		 * @throws IllegalArgumentException if {@code containerPort} is negative or zero
		 */
		@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
		public PortBindingBuilder(int containerPort)
		{
			requireThat(containerPort, "containerPort").isPositive();
			this.containerPort = containerPort;
			this.hostAddress = InetAddress.ofLiteral("0.0.0.0");
		}

		/**
		 * Sets the host address to bind to. By default, {@code 0.0.0.0} is used.
		 *
		 * @param hostAddress the host address
		 * @return this
		 * @throws NullPointerException if {@code hostAddress} is null
		 */
		public PortBindingBuilder hostAddress(InetAddress hostAddress)
		{
			requireThat(hostAddress, "hostAddress").isNotNull();
			this.hostAddress = hostAddress;
			return this;
		}

		/**
		 * Sets the host port to bind to. By default, an arbitrary available port will be used.
		 *
		 * @param hostPort the host port, or {@code 0} to use an arbitrary available port
		 * @return this
		 * @throws IllegalArgumentException if {@code hostPort} is negative
		 */
		public PortBindingBuilder hostPort(int hostPort)
		{
			requireThat(hostPort, "hostPort").isNotNegative();
			this.hostPort = hostPort;
			return this;
		}

		/**
		 * Sets the communication protocol to bind to. By default, {@link Protocol#TCP} will be used.
		 *
		 * @param protocol the communication protocol to use
		 * @return this
		 * @throws NullPointerException if {@code protocol} is null
		 */
		public PortBindingBuilder protocol(Protocol protocol)
		{
			requireThat(protocol, "protocol").isNotNull();
			this.protocol = protocol;
			return this;
		}

		/**
		 * Builds and returns the {@link PortBinding} configuration.
		 *
		 * @return the configuration
		 */
		public PortBinding build()
		{
			return new PortBinding(containerPort, hostAddress, hostPort, protocol);
		}
	}

	/**
	 * The configuration for binding a container port to a host port.
	 *
	 * @param containerPort the container port
	 * @param hostAddress   the host address
	 * @param hostPort      the host port
	 * @param protocol      the communication protocol to use
	 */
	record PortBinding(int containerPort, InetAddress hostAddress, int hostPort, Protocol protocol)
	{
		/**
		 * Creates a PortBinding.
		 *
		 * @param containerPort the container port
		 * @param hostAddress   the host address
		 * @param hostPort      the host port
		 * @param protocol      the communication protocol to use
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code containerPort} or {@code hostPort} are negative
		 */
		public PortBinding
		{
			requireThat(containerPort, "containerPort").isNotNegative();
			requireThat(hostAddress, "hostAddress").isNotNull();
			requireThat(hostPort, "hostPort").isNotNegative();
			requireThat(protocol, "protocol").isNotNull();
		}
	}

	/**
	 * A network port and protocol.
	 *
	 * @param port     a networking port
	 * @param protocol the protocol
	 */
	record PortAndProtocol(int port, Protocol protocol)
	{
		/**
		 * Creates a port and protocol.
		 *
		 * @param port     an Internet address
		 * @param protocol the protocol
		 */
		public PortAndProtocol
		{
			requireThat(port, "port").isPositive();
			requireThat(protocol, "protocol").isNotNull();
		}
	}

	/**
	 * Determines when the container should automatically restart.
	 *
	 * @param condition       the conditions under which the container is restarted automatically
	 * @param maximumAttempts if {@link RestartPolicyCondition#ON_FAILURE} is used, the number of times to retry
	 *                        before giving up
	 * @see <a
	 * 	href="https://docs.docker.com/engine/containers/start-containers-automatically/#use-a-restart-policy">Docker
	 * 	documentation</a>
	 */
	record RestartPolicy(RestartPolicyCondition condition, int maximumAttempts)
	{
		/**
		 * Creates a restart policy.
		 *
		 * @param condition       the conditions under which the container is restarted automatically
		 * @param maximumAttempts the maximum number of times to restart
		 * @throws NullPointerException     if {@code condition} is null
		 * @throws IllegalArgumentException if {@code condition} is {@code ON_FAILURE} and {@code maximumAttempts}
		 *                                  is negative or zero
		 */
		public RestartPolicy
		{
			requireThat(condition, "condition").isNotNull();
			if (condition == RestartPolicyCondition.ON_FAILURE)
				requireThat(maximumAttempts, "maximumAttempts").isPositive();
		}
	}

	/**
	 * Determines when a container is restarted automatically.
	 */
	enum RestartPolicyCondition
	{
		/**
		 * Do not automatically restart.
		 */
		NO,
		/**
		 * Always restart on exit.
		 */
		ALWAYS,
		/**
		 * Restart the container unless it is manually stopped.
		 */
		UNLESS_STOPPED,
		/**
		 * Restart the container if its exit code is non-zero.
		 */
		ON_FAILURE;

		/**
		 * Returns the command-line representation of this option.
		 *
		 * @return the command-line value
		 */
		public String toCommandLine()
		{
			return name().toLowerCase(Locale.ROOT).replace('_', '-');
		}
	}
}