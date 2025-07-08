package io.github.cowwoc.anchor4j.digitalocean.registry.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.core.exception.AccessDeniedException;
import io.github.cowwoc.anchor4j.digitalocean.registry.internal.client.DefaultRegistryClient;
import io.github.cowwoc.anchor4j.digitalocean.registry.resource.ContainerImage;
import io.github.cowwoc.anchor4j.digitalocean.registry.resource.ContainerRepository;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultContainerImage implements ContainerImage
{
	private final DefaultRegistryClient client;
	private final ContainerRepository repository;
	private final Id id;
	private final Set<String> tags;
	private final Set<Layer> layers;

	/**
	 * Creates a snapshot of the docker image's state.
	 *
	 * @param client     the client configuration
	 * @param repository the docker repository that the image is in
	 * @param id         a value that uniquely identifies the image in the repository
	 * @param tags       the tags that are associated with the image
	 * @param layers     the layers that the image consists of
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code id} contains leading or trailing whitespace or is empty
	 */
	public DefaultContainerImage(DefaultRegistryClient client, ContainerRepository repository, Id id,
		Set<String> tags, Set<Layer> layers)
	{
		assert client != null;
		requireThat(repository, "repository").isNotNull();
		requireThat(id, "id").isNotNull();

		this.client = client;
		this.repository = repository;
		this.id = id;
		this.tags = Set.copyOf(tags);
		this.layers = Set.copyOf(layers);
	}

	@Override
	public ContainerRepository getRepository()
	{
		return repository;
	}

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public Set<String> getTags()
	{
		return tags;
	}

	@Override
	public Set<Layer> getLayers()
	{
		return layers;
	}

	@Override
	public ContainerImage reload() throws IOException, InterruptedException
	{
		return repository.getImageByPredicate(image -> image.getId().equals(id));
	}

	@Override
	public void destroy() throws IOException, InterruptedException, AccessDeniedException
	{
		repository.destroyImage(id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(repository, id, tags, layers);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof ContainerImage other && other.getRepository().equals(repository) &&
			other.getId().equals(id) && other.getTags().equals(tags) && other.getLayers().equals(layers);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultContainerImage.class).
			add("repository", repository).
			add("id", id).
			add("tags", tags).
			add("layers", layers).
			toString();
	}
}