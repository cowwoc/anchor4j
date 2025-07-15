package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.ContainerCreator;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.ImageRemover;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultDockerImage implements DockerImage
{
	private final DockerClient client;
	private final Id id;
	private final Map<String, Set<String>> referenceToTags;
	private final Map<String, String> referenceToDigest;

	/**
	 * Creates a DefaultImage.
	 *
	 * @param client            the client configuration
	 * @param id                the image's ID
	 * @param referenceToTags   a mapping from the image's name to its tags
	 * @param referenceToDigest a mapping from the image's name to its digest
	 * @throws NullPointerException     if any of the arguments, including map keys or values, are null
	 * @throws IllegalArgumentException if the map keys or values contain whitespace or are empty
	 */
	public DefaultDockerImage(DockerClient client, Id id, Map<String, Set<String>> referenceToTags,
		Map<String, String> referenceToDigest)
	{
		assert client != null;
		requireThat(id, "id").isNotNull();
		ParameterValidator.validateReferenceParameters(referenceToTags, referenceToDigest);
		this.client = client;
		this.id = id;

		// Create immutable copies
		Map<String, Set<String>> nameToImmutableTags = new HashMap<>();
		for (Entry<String, Set<String>> entry : referenceToTags.entrySet())
			nameToImmutableTags.put(entry.getKey(), Set.copyOf(entry.getValue()));
		this.referenceToTags = Map.copyOf(nameToImmutableTags);
		this.referenceToDigest = Map.copyOf(referenceToDigest);
	}

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public Map<String, Set<String>> referenceToTags()
	{
		return referenceToTags;
	}

	@Override
	public Map<String, String> referenceToDigest()
	{
		return referenceToDigest;
	}

	@Override
	@CheckReturnValue
	public DockerImage reload() throws IOException, InterruptedException
	{
		return client.getImage(id);
	}

	@Override
	public ContainerCreator createContainer()
	{
		return client.createContainer(id);
	}

	@Override
	public DockerImage addTag(String reference) throws IOException, InterruptedException
	{
		client.tagImage(id, reference);
		return this;
	}

	@Override
	public ImageRemover removeTag(String reference)
	{
		return client.removeImageTag(reference);
	}

	@Override
	public ImageRemover remove()
	{
		return client.removeImage(id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, referenceToTags, referenceToDigest);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DockerImage other && other.getId().equals(id) &&
			other.referenceToTags().equals(referenceToTags) &&
			other.referenceToDigest().equals(referenceToDigest);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultDockerImage.class).
			add("id", id).
			add("referenceToTag", referenceToTags).
			add("referenceToDigest", referenceToDigest).
			toString();
	}
}