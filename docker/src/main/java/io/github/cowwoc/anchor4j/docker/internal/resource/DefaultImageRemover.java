package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.ImageRemover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@code ImageRemover}.
 */
public final class DefaultImageRemover implements ImageRemover
{
	private final InternalDocker client;
	private final String id;
	private boolean force;
	private boolean doNotPruneParents;

	/**
	 * Creates a container remover.
	 *
	 * @param client the client configuration
	 * @param id     the image's ID or {@link DockerImage reference}
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code reference}:
	 *                                  <ul>
	 *                                    <li>is empty.</li>
	 *                                    <li>contains any character other than lowercase letters (a–z),
	 *                                    digits (0–9), and the following characters: {@code '.'}, {@code '/'},
	 *                                    {@code ':'}, {@code '_'}, {@code '-'}, {@code '@'}.</li>
	 *                                  </ul>
	 */
	public DefaultImageRemover(InternalDocker client, String id)
	{
		assert client != null;
		ParameterValidator.validateImageReference(id, "id");
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
	public void remove() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/container/rm/
		List<String> arguments = new ArrayList<>(5);
		arguments.add("image");
		arguments.add("rm");
		if (force)
			arguments.add("--force");
		if (doNotPruneParents)
			arguments.add("--no-prune");
		arguments.add(id);
		CommandResult result = client.retry(deadline -> client.run(arguments, deadline));
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