package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.Node;
import io.github.cowwoc.anchor4j.docker.resource.NodeRemover;
import io.github.cowwoc.anchor4j.docker.resource.NodeState;
import io.github.cowwoc.anchor4j.docker.resource.NodeState.Role;
import io.github.cowwoc.anchor4j.docker.resource.TaskState;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * The default implementation of a {@code Node}.
 */
public final class DefaultNode implements Node
{
	private final InternalDocker client;
	private final String id;

	/**
	 * Creates a state.
	 *
	 * @param client the client configuration
	 * @param id     the node's ID
	 */
	public DefaultNode(InternalDocker client, String id)
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
	public NodeState getState() throws IOException, InterruptedException
	{
		return client.getNodeState(id);
	}

	@Override
	public List<TaskState> listTasks() throws IOException, InterruptedException
	{
		return client.listTasksByNode(id);
	}

	@Override
	public Node drain() throws IOException, InterruptedException
	{
		return client.node(client.drainNode(id));
	}

	@Override
	public Node setRole(Role role, Instant deadline)
		throws IOException, InterruptedException, TimeoutException
	{
		return client.node(client.setNodeRole(id, role, deadline));
	}

	@Override
	public NodeRemover remover()
	{
		return client.removeNode(id);
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DefaultNode other && other.id.equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultNode.class).
			add("id", id).
			toString();
	}
}