package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Container;
import io.github.cowwoc.anchor4j.docker.resource.Container.Id;
import io.github.cowwoc.anchor4j.docker.resource.ContainerStopper;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Default implementation of {@code ContainerStopper}.
 */
public final class DefaultContainerStopper implements ContainerStopper
{
	private final InternalDockerClient client;
	private final Id id;
	private String signal = "";
	private Duration timeout;

	/**
	 * Creates a container stopper.
	 *
	 * @param client the client configuration
	 * @param id     the container's ID or name
	 * @throws NullPointerException if any of the arguments are null
	 */
	public DefaultContainerStopper(InternalDockerClient client, Container.Id id)
	{
		assert client != null;
		requireThat(id, "id").isNotNull();
		this.client = client;
		this.id = id;
	}

	@Override
	public ContainerStopper signal(String signal)
	{
		requireThat(signal, "signal").doesNotContainWhitespace();
		this.signal = signal;
		return this;
	}

	@Override
	public ContainerStopper timeout(Duration timeout)
	{
		this.timeout = timeout;
		return this;
	}

	@Override
	public Container apply() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/container/stop/
		List<String> arguments = new ArrayList<>(7);
		arguments.add("container");
		arguments.add("stop");
		arguments.add(id.getValue());
		if (!signal.isEmpty())
		{
			arguments.add("--signal");
			arguments.add(signal);
		}
		if (timeout != null)
		{
			arguments.add("--timeout");
			if (timeout.isNegative())
				arguments.add("-1");
			else
				arguments.add(String.valueOf(timeout.toSeconds()));
		}
		CommandResult result = client.retry(_ -> client.run(arguments));
		client.getContainerParser().stop(result);
		return client.getContainer(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(ContainerStopper.class).
			add("signal", signal).
			add("timeout", timeout).
			toString();
	}
}