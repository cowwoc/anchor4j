package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.NodeRemover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Default implementation of {@code NodeRemover}.
 */
public final class DefaultNodeRemover implements NodeRemover
{
	private final InternalDocker client;
	private final String id;
	private boolean force;

	/**
	 * Creates a container remover.
	 *
	 * @param client the client configuration
	 * @param id     the node's ID or hostname
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 */
	public DefaultNodeRemover(InternalDocker client, String id)
	{
		assert client != null;
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();
		this.client = client;
		this.id = id;
	}

	@Override
	public NodeRemover force()
	{
		this.force = true;
		return this;
	}

	@Override
	public void remove() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/node/rm/
		List<String> arguments = new ArrayList<>(4);
		arguments.add("node");
		arguments.add("rm");
		if (force)
			arguments.add("--force");
		arguments.add(id);
		CommandResult result = client.retry(deadline -> client.run(arguments, deadline));
		client.getNodeParser().remove(result);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultNodeRemover.class).
			add("id", id).
			add("force", force).
			toString();
	}
}