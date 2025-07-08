package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.ImageRemover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Default implementation of {@code ImageRemover}.
 */
public final class DefaultImageRemover implements ImageRemover
{
	private final InternalDockerClient client;
	private final ContainerImage.Id id;
	private boolean force;
	private boolean doNotPruneParents;

	/**
	 * Creates a container remover.
	 *
	 * @param client the client configuration
	 * @param id     the image's ID or {@link ContainerImage reference}
	 * @throws NullPointerException if {@code id} is null
	 */
	public DefaultImageRemover(InternalDockerClient client, ContainerImage.Id id)
	{
		assert client != null;
		requireThat(id, "id").isNotNull();
		this.client = client;
		this.id = id;
	}

	@Override
	public ImageRemover force()
	{
		this.force = true;
		return this;
	}

	@Override
	public ImageRemover doNotPruneParents()
	{
		this.doNotPruneParents = true;
		return this;
	}

	@Override
	public void apply() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/container/rm/
		List<String> arguments = new ArrayList<>(5);
		arguments.add("image");
		arguments.add("rm");
		if (force)
			arguments.add("--force");
		if (doNotPruneParents)
			arguments.add("--no-prune");
		arguments.add(id.getValue());
		CommandResult result = client.retry(_ -> client.run(arguments));
		client.getImageParser().remove(result);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultImageRemover.class).
			add("force", force).
			add("doNotPruneParents", doNotPruneParents).
			toString();
	}
}