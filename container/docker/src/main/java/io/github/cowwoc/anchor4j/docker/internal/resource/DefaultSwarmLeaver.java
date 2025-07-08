package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.SwarmLeaver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class DefaultSwarmLeaver implements SwarmLeaver
{
	private final InternalDockerClient client;
	private boolean force;

	/**
	 * Creates a swarm leaver.
	 *
	 * @param client the client configuration
	 */
	public DefaultSwarmLeaver(InternalDockerClient client)
	{
		assert client != null;
		this.client = client;
	}

	@Override
	public SwarmLeaver force()
	{
		this.force = true;
		return this;
	}

	@Override
	public void apply() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/swarm/leave/
		List<String> arguments = new ArrayList<>(3);
		arguments.add("swarm");
		arguments.add("leave");
		if (force)
			arguments.add("--force");
		CommandResult result = client.retry(_ -> client.run(arguments));
		client.getSwarmParser().leave(result);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultSwarmLeaver.class).
			add("force", force).
			toString();
	}
}