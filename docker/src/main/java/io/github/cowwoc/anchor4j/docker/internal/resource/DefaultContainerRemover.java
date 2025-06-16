package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.ContainerRemover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The default implementation of {@code ContainerRemover}.
 */
public final class DefaultContainerRemover implements ContainerRemover
{
	private final InternalDocker client;
	private final String id;
	private boolean force;
	private boolean volumes;

	/**
	 * Creates a container remover.
	 *
	 * @param id     the container's ID or name
	 * @param client the client configuration
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id}'s format is invalid
	 */
	public DefaultContainerRemover(InternalDocker client, String id)
	{
		assert client != null;
		ParameterValidator.validateContainerIdOrName(id, "id");
		this.client = client;
		this.id = id;
	}

	@Override
	public ContainerRemover kill()
	{
		this.force = true;
		return this;
	}

	@Override
	public ContainerRemover removeAnonymousVolumes()
	{
		this.volumes = true;
		return this;
	}

	@Override
	public void remove() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/container/rm/
		List<String> arguments = new ArrayList<>(5);
		arguments.add("container");
		arguments.add("rm");
		if (force)
			arguments.add("--force");
		if (volumes)
			arguments.add("--volumes");
		arguments.add(id);
		CommandResult result = client.retry(deadline -> client.run(arguments, deadline));
		client.getContainerParser().remove(result);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(ContainerRemover.class).
			add("force", force).
			add("volumes", volumes).
			toString();
	}
}