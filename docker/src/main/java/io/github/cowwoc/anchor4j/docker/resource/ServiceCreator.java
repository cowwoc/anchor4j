package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.docker.resource.ContainerCreator.BindMountOptions;
import io.github.cowwoc.anchor4j.docker.resource.ContainerCreator.PortBinding;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Creates a service, representing a group of containers.
 */
public interface ServiceCreator
{
	/**
	 * Sets the name of the container.
	 *
	 * @param name the container name. The value must start with a letter, or digit, or underscore, and may be
	 *             followed by up by additional characters consisting of letters, digits, underscores, periods
	 *             or hyphens.
	 * @return this
	 * @throws NullPointerException     if {@code name} is null
	 * @throws IllegalArgumentException if {@code name}'s format is invalid
	 */
	ServiceCreator name(String name);

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
	ServiceCreator entrypoint(String... entrypoint);

	/**
	 * Sets the container's {@code CMD}, overriding any value set in the Dockerfile or becoming the full command
	 * if the Dockerfile does not contain an {@code ENTRYPOINT}, and no new {@link #entrypoint(String...)} is
	 * specified.
	 *
	 * @param arguments the arguments
	 * @return this
	 * @throws NullPointerException     if {@code arguments} is null
	 * @throws IllegalArgumentException if {@code arguments} contains whitespace or is empty
	 */
	ServiceCreator arguments(String... arguments);

	/**
	 * Sets the working directory to run commands in.
	 *
	 * @param workingDirectory the working directory
	 * @return this
	 * @throws NullPointerException     if {@code workingDirectory} is null
	 * @throws IllegalArgumentException if {@code workingDirectory} contains whitespace or is empty
	 */
	ServiceCreator workingDirectory(String workingDirectory);

	/**
	 * Adds or replaces an environment variable.
	 *
	 * @param name  the name of the variable
	 * @param value the value of the variable
	 * @return this
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if any of the arguments contain whitespace
	 */
	ServiceCreator environmentVariable(String name, String value);

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
	ServiceCreator bindPath(Path hostPath, String containerPath, BindMountOptions... options);

	/**
	 * Binds a container port to a host port.
	 *
	 * @param configuration the port binding configuration
	 * @return this
	 * @throws NullPointerException if {@code configuration} is null
	 */
	ServiceCreator bindPort(PortBinding configuration);

	/**
	 * Indicates that the service should run a fixed number of replicas (copies) of the task. By default, only a
	 * single replica is run.
	 *
	 * @param replicas the number of copies
	 * @return this
	 * @throws IllegalArgumentException if {@code replicas} is negative or zero
	 * @see #runOncePerNode()
	 */
	ServiceCreator runMultipleCopies(int replicas);

	/**
	 * Indicates that the service should run the task on each active node in the swarm. By default, the service
	 * only runs a specified number of replicas of the task.
	 *
	 * @return this
	 * @see #runMultipleCopies(int)
	 */
	ServiceCreator runOncePerNode();

	/**
	 * Indicates that the tasks are expected to run to completion exactly once and are not restarted afterward.
	 * <p>
	 * Once a task exits, it will not be restarted; even if the number of replicas is scaled or the node
	 * restarts. This is useful for batch jobs or one-off processes that should not restart on failure or
	 * shutdown.
	 * <p>
	 * By default, tasks are restarted within five seconds of exiting, unless the service is removed.
	 *
	 * @return this
	 */
	ServiceCreator runOnce();

	/**
	 * Indicates that the container should automatically restart when it stops, regardless of the reason. By
	 * default, tasks are restarted automatically five seconds after shutting down.
	 *
	 * @param maximumAttempts the number of times to retry before giving up
	 * @param delay           the amount of time to wait after a task has exited before attempting to restart
	 *                        it
	 * @param slidingWindow   the time period during which failures are counted towards {@code maximumAttempts}.
	 *                        If the task fails more than {@code maximumAttempts} times within this period, it
	 *                        will not be restarted again. If set to zero, all failures are counted without any
	 *                        time constraint.
	 * @return this
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code maximumAttempts}, {@code delay} or {@code slidingWindow} are
	 *                                  negative
	 */
	ServiceCreator alwaysRestart(int maximumAttempts, Duration delay, Duration slidingWindow);

	/**
	 * Configures the container to restart if its exit code is non-zero.
	 *
	 * @param maximumAttempts the number of times to retry before giving up
	 * @param delay           the amount of time to wait after a task has exited before attempting to restart
	 *                        it
	 * @param slidingWindow   the time period during which failures are counted towards {@code maximumAttempts}.
	 *                        If the task fails more than {@code maximumAttempts} times within this period, it
	 *                        will not be restarted again. If set to zero, all failures are counted without any
	 *                        time constraint.
	 * @return this
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code maximumAttempts}, {@code delay} or {@code slidingWindow} are
	 *                                  negative
	 * @throws IllegalArgumentException if {@code maximumAttempts} is negative
	 */
	ServiceCreator restartOnFailure(int maximumAttempts, Duration delay, Duration slidingWindow);

	/**
	 * Specifies the duration to monitor each task for failures after starting. By default, tasks are monitored
	 * for 5 seconds after startup.
	 * <p>
	 * This parameter is useful for detecting unstable or crashing containers during rolling updates.
	 *
	 * @param updateMonitor The duration to monitor updated tasks for failure
	 * @return this
	 * @throws NullPointerException     if {@code updateMonitor} is null
	 * @throws IllegalArgumentException if {@code updateMonitor} is negative or zero
	 */
	ServiceCreator updateMonitor(Duration updateMonitor);

	/**
	 * Creates the service.
	 *
	 * @return the new service
	 * @throws ResourceNotFoundException if the referenced image is not available locally and cannot be pulled
	 *                                   from Docker Hub, either because the repository does not exist or
	 *                                   requires different authentication credentials
	 * @throws ResourceInUseException    if the requested name is in use by another service
	 * @throws NotSwarmManagerException  if the current node is not a swarm manager
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	Service create() throws IOException, InterruptedException;

	/**
	 * A mode of operation.
	 */
	enum Mode
	{
		/**
		 * The service runs a specific number copies (replicas) of the task.
		 */
		REPLICATED,
		/**
		 * The service runs the task on every active node.
		 */
		GLOBAL
	}

	/**
	 * Determines when the container should automatically restart.
	 *
	 * @param condition       the conditions under which the container is restarted automatically
	 * @param maximumAttempts the number of times to retry before giving up
	 * @param delay           the amount of time to wait after a task has exited before attempting to restart
	 *                        it
	 * @param slidingWindow   the time period during which failures are counted towards {@code maximumAttempts}.
	 *                        If the task fails more than {@code maximumAttempts} times within this period, it
	 *                        will not be restarted again. If set to zero, all failures are counted without any
	 *                        time constraint.
	 */
	record RestartPolicy(RestartPolicyCondition condition, Duration delay, int maximumAttempts,
	                     Duration slidingWindow)
	{
		/**
		 * Creates a restart policy.
		 *
		 * @param condition       the conditions under which the container is restarted automatically
		 * @param maximumAttempts the number of times to retry before giving up
		 * @param delay           the amount of time to wait after a task has exited before attempting to restart
		 *                        it
		 * @param slidingWindow   the time period during which failures are counted towards
		 *                        {@code maximumAttempts}. If the task fails more than {@code maximumAttempts}
		 *                        times within this period, it will not be restarted again. If set to zero, all
		 *                        failures are counted without any time constraint.
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code condition} is {@code ALWAYS} or {@code ON_FAILURE}, and
		 *                                  {@code maximumAttempts}, {@code delay} or {@code window} are negative
		 */
		public RestartPolicy
		{
			requireThat(condition, "condition").isNotNull();
			switch (condition)
			{
				case NONE ->
				{
				}
				case ANY, ON_FAILURE ->
				{
					requireThat(delay, "delay").isGreaterThanOrEqualTo(Duration.ZERO);
					requireThat(maximumAttempts, "maximumAttempts").isNotNegative();
					requireThat(slidingWindow, "slidingWindow").isGreaterThanOrEqualTo(Duration.ZERO);
				}
			}
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
		NONE,
		/**
		 * Always restart on exit.
		 */
		ANY,
		/**
		 * Restart the container if its exit code is non-zero.
		 */
		ON_FAILURE;

//		/**
//		 * Returns the command-line representation of this option.
//		 *
//		 * @return the command-line value
//		 */
//		public String toCommandLine()
//		{
//			return name().toLowerCase(Locale.ROOT).replace('_', '-');
//		}
	}
}