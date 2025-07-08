package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Container;
import io.github.cowwoc.anchor4j.docker.resource.ContainerCreator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import static io.github.cowwoc.anchor4j.docker.resource.Protocol.TCP;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultContainerCreator implements ContainerCreator
{
	/**
	 * The format of container names.
	 */
	static final Pattern NAME_PATTERN = Pattern.compile("\\w[\\w.-]{0,127}");
	private final InternalDockerClient client;
	private final ContainerImage.Id imageId;
	private String name = "";
	private String platform = "";
	private boolean privileged;
	private List<String> entrypoint = List.of();
	private List<String> arguments = List.of();
	private String workingDirectory = "";
	private final Map<String, String> environmentVariables = new HashMap<>();
	private final Map<Path, BindMount> hostPathToBindMount = new HashMap<>();
	private final Map<PortAndProtocol, InetSocketAddress> containerToHostPort = new HashMap<>();
	private boolean removeOnExit;
	private RestartPolicy restartPolicy = new RestartPolicy(RestartPolicyCondition.NO, 0);

	/**
	 * Creates a container creator.
	 *
	 * @param client  the client configuration
	 * @param imageId the image ID or {@link ContainerImage reference} to create the container from
	 * @throws NullPointerException if {@code imageId} is null
	 */
	public DefaultContainerCreator(InternalDockerClient client, ContainerImage.Id imageId)
	{
		assert client != null;
		requireThat(imageId, "imageId").isNotNull();
		this.client = client;
		this.imageId = imageId;
	}

	@Override
	public ContainerCreator name(String name)
	{
		ParameterValidator.validateName(name, "name");
		if (!name.isEmpty() && !NAME_PATTERN.matcher(name).matches())
		{
			throw new IllegalArgumentException("name must start with a letter, or digit, or underscore, and may " +
				"be followed by additional characters consisting of letters, digits, underscores, periods or " +
				"hyphens.\n" +
				"Actual: " + name);
		}
		this.name = name;
		return this;
	}

	@Override
	public ContainerCreator platform(String platform)
	{
		requireThat(platform, "platform").doesNotContainWhitespace().isNotEmpty();
		this.platform = platform;
		return this;
	}

	@Override
	public ContainerCreator entrypoint(String... entrypoint)
	{
		return entrypoint(Arrays.asList(entrypoint));
	}

	@Override
	public ContainerCreator entrypoint(List<String> entrypoint)
	{
		requireThat(entrypoint, "entrypoint").size().isGreaterThanOrEqualTo(1);
		for (String element : entrypoint)
		{
			requireThat(element, "element").withContext(entrypoint, "entrypoint").
				doesNotContainWhitespace().isNotEmpty();
		}
		this.entrypoint = List.copyOf(entrypoint);
		return this;
	}

	@Override
	public ContainerCreator arguments(String... arguments)
	{
		return arguments(Arrays.asList(arguments));
	}

	@Override
	public ContainerCreator arguments(List<String> arguments)
	{
		requireThat(arguments, "arguments").isNotNull();
		this.arguments = List.copyOf(arguments);
		return this;
	}

	@Override
	public ContainerCreator workingDirectory(String workingDirectory)
	{
		requireThat(workingDirectory, "workingDirectory").doesNotContainWhitespace().isNotEmpty();
		this.workingDirectory = workingDirectory;
		return this;
	}

	@Override
	public ContainerCreator environmentVariable(String name, String value)
	{
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();
		requireThat(value, "value").doesNotContainWhitespace();
		this.environmentVariables.put(name, value);
		return this;
	}

	@Override
	public ContainerCreator bindPath(Path hostPath, String containerPath, BindMountOptions... options)
	{
		// REMINDER: containerPath is not a Path because Paths are resolved relative to the host
		requireThat(hostPath, "hostPath").isNotNull();
		this.hostPathToBindMount.put(hostPath.toAbsolutePath(),
			new BindMount(containerPath, options));
		return this;
	}

	@Override
	public ContainerCreator bindPort(PortBinding portBinding)
	{
		requireThat(portBinding, "portBinding").isNotNull();
		containerToHostPort.put(new PortAndProtocol(portBinding.containerPort(), portBinding.protocol()),
			new InetSocketAddress(portBinding.hostAddress(), portBinding.hostPort()));
		return this;
	}

	@Override
	public ContainerCreator removeOnExit()
	{
		if (restartPolicy.condition() != RestartPolicyCondition.NO)
			throw new IllegalArgumentException("removeOnExit cannot be enabled if restartPolicy is set");
		this.removeOnExit = true;
		return this;
	}

	@Override
	public ContainerCreator alwaysRestart()
	{
		if (removeOnExit)
			throw new IllegalArgumentException("restartPolicy may not be set if removeOnExit is enabled");
		this.restartPolicy = new RestartPolicy(RestartPolicyCondition.ALWAYS, Integer.MAX_VALUE);
		return this;
	}

	@Override
	public ContainerCreator restartUnlessStopped()
	{
		if (removeOnExit)
			throw new IllegalArgumentException("restartPolicy may not be set if removeOnExit is enabled");
		this.restartPolicy = new RestartPolicy(RestartPolicyCondition.UNLESS_STOPPED, Integer.MAX_VALUE);
		return this;
	}

	@Override
	public ContainerCreator restartOnFailure(int maximumAttempts)
	{
		if (removeOnExit)
			throw new IllegalArgumentException("restartPolicy may not be set if removeOnExit is enabled");
		this.restartPolicy = new RestartPolicy(RestartPolicyCondition.ON_FAILURE, maximumAttempts);
		return this;
	}

	@Override
	public ContainerCreator privileged()
	{
		// Documentation taken from https://docs.docker.com/reference/cli/docker/container/run/#privileged
		this.privileged = true;
		return this;
	}

	@Override
	public Container apply() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/container/create/
		List<String> arguments = new ArrayList<>(4 + environmentVariables.size() * 2 +
			hostPathToBindMount.size() * 2 + 5 + containerToHostPort.size() * 2 + 3 + entrypoint.size() +
			this.arguments.size());
		arguments.add("container");
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
		if (!platform.isEmpty())
		{
			arguments.add("--platform");
			arguments.add(platform);
		}
		if (privileged)
			arguments.add("--privileged");
		addBindPortArguments(arguments);
		if (restartPolicy.condition() != RestartPolicyCondition.NO)
		{
			arguments.add("--restart");
			StringBuilder value = new StringBuilder(restartPolicy.condition().toCommandLine());
			if (restartPolicy.condition() == RestartPolicyCondition.ON_FAILURE)
				value.append(':').append(restartPolicy.maximumAttempts());
			arguments.add(value.toString());
		}
		if (removeOnExit)
			arguments.add("--rm");
		if (!workingDirectory.isEmpty())
		{
			arguments.add("--workdir");
			arguments.add(workingDirectory);
		}
		arguments.add(imageId.getValue());
		if (entrypoint.size() > 1)
			arguments.addAll(entrypoint.subList(1, entrypoint.size()));
		if (!this.arguments.isEmpty())
			arguments.addAll(this.arguments);
		CommandResult result = client.retry(_ -> client.run(arguments));
		return client.getContainer(client.getContainerParser().create(result));
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

			BindMount bind = entry.getValue();
			options.add("target=" + bind.containerPath());
			for (BindMountOptions option : bind.options())
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
		return new ToStringBuilder(DefaultContainerCreator.class).
			add("name", name).
			add("platform", platform).
			add("privileged", privileged).
			add("entrypoint", entrypoint).
			add("arguments", arguments).
			add("workingDirectory", workingDirectory).
			add("environmentVariables", environmentVariables).
			add("hostPathToBindMount", hostPathToBindMount).
			add("containerToHostPort", containerToHostPort).
			add("removeOnExit", removeOnExit).
			add("restartPolicy", restartPolicy).
			toString();
	}
}