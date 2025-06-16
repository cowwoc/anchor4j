package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
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
	private final InternalDocker client;
	private final String name;
	private boolean force;

	/**
	 * Creates a context remover.
	 *
	 * @param client the client configuration
	 * @param name   the name of the context
	 * @throws NullPointerException     if {@code name} is null
	 * @throws IllegalArgumentException if {@code name}'s format is invalid
	 */
	public DefaultContextRemover(InternalDocker client, String name)
	{
		assert client != null;
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();
		this.client = client;
		this.name = name;
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
		arguments.add(name);
		CommandResult result = client.retry(deadline -> client.run(arguments, deadline));
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