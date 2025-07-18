package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage.Id;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The properties used by the predicate in {@link DockerClient#getImages(Predicate)}.
 *
 * @param id                the image's ID
 * @param referenceToTags   a mapping from the image's reference to its tags
 * @param referenceToDigest a mapping from the image's reference to its digest
 */
public record DockerImageElement(Id id, Map<String, Set<String>> referenceToTags,
                                 Map<String, String> referenceToDigest)
{
	/**
	 * Creates an image element.
	 *
	 * @param id                the image's ID
	 * @param referenceToTags   a mapping from the image's reference to its tags
	 * @param referenceToDigest a mapping from the image's reference to its digest
	 * @throws NullPointerException     if any of the arguments, including map keys or values, are null
	 * @throws IllegalArgumentException if the map keys or values contain whitespace or are empty
	 */
	public DockerImageElement(Id id, Map<String, Set<String>> referenceToTags,
		Map<String, String> referenceToDigest)
	{
		assert id != null;
		ParameterValidator.validateReferenceParameters(referenceToTags, referenceToDigest);

		this.id = id;
		this.referenceToTags = Map.copyOf(referenceToTags);
		this.referenceToDigest = Map.copyOf(referenceToDigest);
	}
}