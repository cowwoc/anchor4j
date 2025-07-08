package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Node;
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
	private final InternalDockerClient client;
	private final Node.Id id;
	private boolean force;

	/**
	 * Creates a container remover.
	 *
	 * @param client the client configuration
	 * @param id     the node's ID or hostname
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 */
	public DefaultNodeRemover(InternalDockerClient client, Node.Id id)
	{
		assert client != null;
		requireThat(id, "id").isNotNull();
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
	public void apply() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/node/rm/
		List<String> arguments = new ArrayList<>(4);
		arguments.add("node");
		arguments.add("rm");
		if (force)
			arguments.add("--force");
		arguments.add(id.getValue());
		CommandResult result = client.retry(_ -> client.run(arguments));
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