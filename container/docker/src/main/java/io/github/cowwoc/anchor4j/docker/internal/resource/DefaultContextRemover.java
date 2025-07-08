package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Context;
import io.github.cowwoc.anchor4j.docker.resource.ContextRemover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Default implementation of {@code ContextRemover}.
 */
public final class DefaultContextRemover implements ContextRemover
{
	private final InternalDockerClient client;
	private final Context.Id id;
	private boolean force;

	/**
	 * Creates a context remover.
	 *
	 * @param client the client configuration
	 * @param id     the ID of the context
	 * @throws NullPointerException if {@code id} is null
	 */
	public DefaultContextRemover(InternalDockerClient client, Context.Id id)
	{
		assert client != null;
		requireThat(id, "id").isNotNull();
		this.client = client;
		this.id = id;
	}

	@Override
	public ContextRemover force()
	{
		this.force = true;
		return this;
	}

	@Override
	public void remove() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/context/rm/
		List<String> arguments = new ArrayList<>(4);
		arguments.add("context");
		arguments.add("rm");
		if (force)
			arguments.add("--force");
		arguments.add(id.getValue());
		CommandResult result = client.retry(_ -> client.run(arguments));
		client.getContextParser().remove(result);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultContextRemover.class).
			add("force", force).
			toString();
	}
}