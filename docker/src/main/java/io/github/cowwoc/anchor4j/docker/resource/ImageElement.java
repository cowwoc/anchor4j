package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.Docker;

import java.util.Map;
import java.util.Set;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * An element returned by {@link Docker#listImages()}.
 *
 * @param image             the image
 * @param referenceToTags   a mapping from the image's reference to its tags
 * @param referenceToDigest a mapping from the image's reference to its digest
 */
public record ImageElement(DockerImage image, Map<String, Set<String>> referenceToTags,
                           Map<String, String> referenceToDigest)
{
	/**
	 * Creates an image element.
	 *
	 * @param image             the image
	 * @param referenceToTags   a mapping from the image's reference to its tags
	 * @param referenceToDigest a mapping from the image's reference to its digest
	 * @throws NullPointerException     if any of the arguments, including map keys or values, are null
	 * @throws IllegalArgumentException if the map keys or values contain whitespace or are empty
	 */
	public ImageElement(DockerImage image, Map<String, Set<String>> referenceToTags,
		Map<String, String> referenceToDigest)
	{
		requireThat(image, "image").isNotNull();
		ImageState.validateReferenceParameters(referenceToTags, referenceToDigest);
		this.image = image;
		this.referenceToTags = Map.copyOf(referenceToTags);
		this.referenceToDigest = Map.copyOf(referenceToDigest);
	}
}