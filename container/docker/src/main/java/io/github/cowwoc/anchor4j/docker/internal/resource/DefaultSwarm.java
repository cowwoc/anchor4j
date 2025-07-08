package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.JoinToken;
import io.github.cowwoc.anchor4j.docker.resource.Swarm;
import io.github.cowwoc.anchor4j.docker.resource.SwarmLeaver;

import java.io.IOException;

public final class DefaultSwarm implements Swarm
{
	private final InternalDockerClient client;

	/**
	 * Creates a reference to a Swarm.
	 *
	 * @param client the client configuration
	 */
	public DefaultSwarm(InternalDockerClient client)
	{
		assert client != null;
		this.client = client;
	}

	@Override
	public SwarmLeaver leave()
	{
		return client.leaveSwarm();
	}

	@Override
	public JoinToken getManagerJoinToken() throws IOException, InterruptedException
	{
		return client.getManagerJoinToken();
	}

	@Override
	public JoinToken getWorkerJoinToken() throws IOException, InterruptedException
	{
		return client.getWorkerJoinToken();
	}

	@Override
	public int hashCode()
	{
		return 0;
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Swarm;
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DockerImage.class).
			toString();
	}
}