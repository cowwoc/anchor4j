package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.ContainerCreator.BindMount;
import io.github.cowwoc.anchor4j.docker.resource.ContainerCreator.BindMountOptions;
import io.github.cowwoc.anchor4j.docker.resource.ContainerCreator.PortAndProtocol;
import io.github.cowwoc.anchor4j.docker.resource.ContainerCreator.PortBinding;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.Service;
import io.github.cowwoc.anchor4j.docker.resource.ServiceCreator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import static io.github.cowwoc.anchor4j.docker.resource.Protocol.TCP;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * Default implementation of a {@code ServiceCreator}.
 */
public final class DefaultServiceCreator implements ServiceCreator
{
	private static final RestartPolicy DEFAULT_RESTART_POLICY = new RestartPolicy(RestartPolicyCondition.ANY,
		Duration.ofSeconds(5), Integer.MAX_VALUE, Duration.ZERO);
	private final InternalDocker client;
	private final String imageId;
	private String name = "";
	private List<String> entrypoint = List.of();
	private List<String> arguments = List.of();
	private String workingDirectory = "";
	private final Map<String, String> environmentVariables = new HashMap<>();
	private final Map<Path, BindMount> hostPathToBindMount = new HashMap<>();
	private final Map<PortAndProtocol, InetSocketAddress> containerToHostPort = new HashMap<>();
	private boolean runOncePerNode;
	private int numberOfReplicas;
	private RestartPolicy restartPolicy = DEFAULT_RESTART_POLICY;
	private Duration updateMonitor = Duration.ofSeconds(5);

	/**
	 * Creates a container creator.
	 *
	 * @param client  the client configuration
	 * @param imageId the image ID or {@link DockerImage reference} to create containers from
	 * @throws NullPointerException     if {@code imageId} is null
	 * @throws IllegalArgumentException if {@code imageId}:
	 *                                  <ul>
	 *                                    <li>is empty.</li>
	 *                                    <li>contains any character other than lowercase letters (a–z),
	 *                                    digits (0-9) and the following characters: {@code '.'}, {@code '/'},
	 *                                    {@code ':'}, {@code '_'}, {@code '-'}, {@code '@'}.</li>
	 *                                  </ul>
	 */
	public DefaultServiceCreator(InternalDocker client, String imageId)
	{
		assert client != null;
		ParameterValidator.validateImageReference(imageId, "imageId");
		this.client = client;
		this.imageId = imageId;
	}

	@Override
	public ServiceCreator name(String name)
	{
		ParameterValidator.validateName(name, "name");
		this.name = name;
		return this;
	}

	@Override
	public ServiceCreator entrypoint(String... entrypoint)
	{
		requireThat(entrypoint, "entrypoint").length().isGreaterThanOrEqualTo(1);
		for (String element : entrypoint)
		{
			requireThat(element, "element").withContext(entrypoint, "entrypoint").
				doesNotContainWhitespace().isNotEmpty();
		}
		this.entrypoint = List.copyOf(Arrays.asList(entrypoint));
		return this;
	}

	@Override
	public ServiceCreator arguments(String... arguments)
	{
		requireThat(arguments, "arguments").isNotNull();
		this.arguments = List.copyOf(Arrays.asList(arguments));
		return this;
	}

	@Override
	public ServiceCreator workingDirectory(String workingDirectory)
	{
		requireThat(workingDirectory, "workingDirectory").doesNotContainWhitespace().isNotEmpty();
		this.workingDirectory = workingDirectory;
		return this;
	}

	@Override
	public ServiceCreator environmentVariable(String name, String value)
	{
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();
		requireThat(value, "value").doesNotContainWhitespace();
		this.environmentVariables.put(name, value);
		return this;
	}

	@Override
	public ServiceCreator bindPath(Path hostPath, String containerPath, BindMountOptions... options)
	{
		// REMINDER: containerPath is not a Path because Paths are resolved relative to the host
		requireThat(hostPath, "hostPath").isNotNull();
		this.hostPathToBindMount.put(hostPath.toAbsolutePath(),
			new BindMount(containerPath, options));
		return this;
	}

	@Override
	public ServiceCreator bindPort(PortBinding configuration)
	{
		requireThat(configuration, "configuration").isNotNull();
		containerToHostPort.put(new PortAndProtocol(configuration.containerPort(), configuration.protocol()),
			new InetSocketAddress(configuration.hostAddress(), configuration.hostPort()));
		return this;
	}

	@Override
	public ServiceCreator runMultipleCopies(int replicas)
	{
		this.runOncePerNode = false;
		this.numberOfReplicas = replicas;
		return this;
	}

	@Override
	public ServiceCreator runOncePerNode()
	{
		this.runOncePerNode = true;
		return this;
	}

	@Override
	public ServiceCreator runOnce()
	{
		this.restartPolicy = new RestartPolicy(RestartPolicyCondition.NONE, Duration.ZERO, 0, Duration.ZERO);
		return this;
	}

	@Override
	public ServiceCreator alwaysRestart(int maximumAttempts, Duration delay, Duration slidingWindow)
	{
		this.restartPolicy = new RestartPolicy(RestartPolicyCondition.ANY, delay, maximumAttempts,
			slidingWindow);
		return this;
	}

	@Override
	public ServiceCreator restartOnFailure(int maximumAttempts, Duration delay, Duration slidingWindow)
	{
		this.restartPolicy = new RestartPolicy(RestartPolicyCondition.ON_FAILURE, delay, maximumAttempts,
			slidingWindow);
		return this;
	}

	@Override
	public ServiceCreator updateMonitor(Duration updateMonitor)
	{
		requireThat(updateMonitor, "updateMonitor").isGreaterThan(Duration.ZERO);
		this.updateMonitor = updateMonitor;
		return this;
	}

	@Override
	public Service create() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/service/create/
		List<String> arguments = new ArrayList<>(4 + environmentVariables.size() * 2 +
			hostPathToBindMount.size() * 2 + 5 + containerToHostPort.size() * 2 + 3 + entrypoint.size() + 2 +
			this.arguments.size());
		arguments.add("service");
		arguments.add("create");
		if (!entrypoint.isEmpty())
		{
			arguments.add("--entrypoint");
			// If a user specifies:
			// ENTRYPOINT ["executable", "param1", "param2"]
			// CMD ["param3", "param4"]
			//
			// then the actual command that will be executed will be: "executable param1 param2 param3 param4"
			// To translate this behavior to command-line arguments, we need to pass the first value to --entrypoint
			// and all remaining values as arguments.
			arguments.add(entrypoint.getFirst());
		}
		if (!environmentVariables.isEmpty())
		{
			for (Entry<String, String> entry : environmentVariables.entrySet())
			{
				arguments.add("--env");
				arguments.add(entry.getKey() + "=" + entry.getValue());
			}
		}
		addBindPathArguments(arguments);
		if (!name.isEmpty())
		{
			arguments.add("--name");
			arguments.add(name);
		}
		addBindPortArguments(arguments);
		if (!restartPolicy.equals(DEFAULT_RESTART_POLICY))
		{
			if (restartPolicy.condition() != DEFAULT_RESTART_POLICY.condition())
			{
				arguments.add("--restart-condition");
				arguments.add(restartPolicy.condition().name().toLowerCase(Locale.ROOT));
			}
			if (!restartPolicy.delay().equals(DEFAULT_RESTART_POLICY.delay()))
			{
				arguments.add("--restart-delay");
				arguments.add(toString(restartPolicy.delay()));
			}
			if (restartPolicy.maximumAttempts() != DEFAULT_RESTART_POLICY.maximumAttempts())
			{
				arguments.add("--restart-max-attempts");
				arguments.add(String.valueOf(restartPolicy.maximumAttempts()));
			}
			if (!restartPolicy.slidingWindow().equals(DEFAULT_RESTART_POLICY.slidingWindow()))
			{
				arguments.add("--restart-window");
				arguments.add(toString(restartPolicy.slidingWindow()));
			}
		}
		if (!updateMonitor.equals(Duration.ofSeconds(5)))
		{
			arguments.add("--update-monitor");
			arguments.add(toString(updateMonitor));
		}
		if (!workingDirectory.isEmpty())
		{
			arguments.add("--workdir");
			arguments.add(workingDirectory);
		}
		arguments.add(imageId);
		if (entrypoint.size() > 1)
			arguments.addAll(entrypoint.subList(1, entrypoint.size()));
		if (!this.arguments.isEmpty())
			arguments.addAll(this.arguments);
		CommandResult result = client.retry(deadline -> client.run(arguments, deadline));
		return client.service(client.getServiceParser().create(result));
	}

	/**
	 * @param duration a duration
	 * @return the String representation of the duration
	 */
	private String toString(Duration duration)
	{
		assert that(duration, "duration").isGreaterThanOrEqualTo(Duration.ZERO).elseThrow();
		if (duration.isZero())
			return "0s";

		Duration timeLeft = duration;
		StringBuilder result = new StringBuilder();

		int hours = timeLeft.toHoursPart();
		if (hours > 0)
		{
			result.append(hours).append('h');
			timeLeft = timeLeft.minusHours(hours);
		}

		int minutes = timeLeft.toMinutesPart();
		if (minutes > 0)
		{
			result.append(minutes).append('m');
			timeLeft = timeLeft.minusMinutes(minutes);
		}

		int seconds = timeLeft.toSecondsPart();
		if (seconds > 0)
		{
			result.append(seconds).append('s');
			timeLeft = timeLeft.minusSeconds(seconds);
		}

		int milliseconds = timeLeft.toMillisPart();
		if (milliseconds > 0)
		{
			result.append(milliseconds).append("ms");
			timeLeft = timeLeft.minusMillis(milliseconds);
		}

		int microseconds = Math.toIntExact(timeLeft.dividedBy(ChronoUnit.MICROS.getDuration()));
		if (microseconds > 0)
		{
			result.append(microseconds).append("us");
			timeLeft = timeLeft.minus(ChronoUnit.MICROS.getDuration().multipliedBy(microseconds));
		}

		long nanoseconds = timeLeft.getNano();
		if (nanoseconds > 0)
			result.append(nanoseconds).append("ns");
		return result.toString();
	}

	private void addBindPathArguments(List<String> arguments)
	{
		if (hostPathToBindMount.isEmpty())
			return;
		for (Entry<Path, BindMount> entry : hostPathToBindMount.entrySet())
		{
			// https://docs.docker.com/engine/storage/bind-mounts/#options-for---mount
			arguments.add("--mount");
			StringJoiner options = new StringJoiner(",");
			options.add("type=bind");
			options.add("source=" + entry.getKey());

			BindMount mount = entry.getValue();
			options.add("target=" + mount.containerPath());
			for (BindMountOptions option : mount.options())
				options.add(option.toJson());
			arguments.add(options.toString());
		}
	}

	private void addBindPortArguments(List<String> arguments)
	{
		if (containerToHostPort.isEmpty())
			return;
		for (Entry<PortAndProtocol, InetSocketAddress> entry : containerToHostPort.entrySet())
		{
			PortAndProtocol portAndProtocol = entry.getKey();
			InetSocketAddress host = entry.getValue();
			StringBuilder value = new StringBuilder();
			InetAddress hostAddress = host.getAddress();
			if (!hostAddress.isAnyLocalAddress())
				value.append(hostAddress.getHostAddress()).append(':');
			value.append(host.getPort()).append(':').append(portAndProtocol.port());
			if (portAndProtocol.protocol() != TCP)
				value.append('/').append(portAndProtocol.protocol());

			arguments.add("--publish");
			arguments.add(value.toString());
		}
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultServiceCreator.class).
			add("name", name).
			add("entrypoint", entrypoint).
			add("arguments", arguments).
			add("workingDirectory", workingDirectory).
			add("environmentVariables", environmentVariables).
			add("hostPathToBindMount", hostPathToBindMount).
			add("containerToHostPort", containerToHostPort).
			add("runOnEachActiveNode", runOncePerNode).
			add("numberOfReplicas", numberOfReplicas).
			add("restartPolicy", restartPolicy).
			toString();
	}
}
