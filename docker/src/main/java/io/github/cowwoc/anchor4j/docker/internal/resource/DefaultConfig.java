package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.Config;
import io.github.cowwoc.anchor4j.docker.resource.ConfigState;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;

import java.io.IOException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * The default implementation of {@code Config}.
 */
public final class DefaultConfig implements Config
{
	private final InternalDocker client;
	private final String id;

	/**
	 * Creates a reference to a swarm's configuration.
	 *
	 * @param client the client configuration
	 * @param id     the config's ID
	 */
	public DefaultConfig(InternalDocker client, String id)
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
	public ConfigState getState() throws IOException, InterruptedException
	{
		return client.getConfigState(id);
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DefaultConfig other && other.id.equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DockerImage.class).
			add("id", id).
			toString();
	}
}