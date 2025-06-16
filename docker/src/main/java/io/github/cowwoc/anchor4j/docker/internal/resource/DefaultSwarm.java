package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.JoinToken;
import io.github.cowwoc.anchor4j.docker.resource.Swarm;
import io.github.cowwoc.anchor4j.docker.resource.SwarmLeaver;

import java.io.IOException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * The default implementation of a {@code Swarm}.
 */
public final class DefaultSwarm implements Swarm
{
	private final InternalDocker client;
	private final String id;

	/**
	 * Creates a reference to a Swarm.
	 *
	 * @param client the client configuration
	 * @param id     the ID of the container
	 */
	public DefaultSwarm(InternalDocker client, String id)
	{
		assert client != null;
		assert that(id, "id").doesNotContainWhitespace().isNotEmpty().elseThrow();
		this.client = client;
		this.id = id;
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public SwarmLeaver leaver()
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
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DefaultSwarm other && other.id.equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DockerImage.class).
			add("id", id).
			toString();
	}
}