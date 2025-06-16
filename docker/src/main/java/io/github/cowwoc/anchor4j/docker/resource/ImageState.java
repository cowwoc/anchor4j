package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A snapshot of an image's state.
 */
public final class ImageState
{
	private final Docker client;
	private final String id;
	private final Map<String, Set<String>> referenceToTags;
	private final Map<String, String> referenceToDigest;

	/**
	 * Creates an ImageState.
	 *
	 * @param client            the client configuration
	 * @param id                the image's ID
	 * @param referenceToTags   a mapping from the image's name to its tags
	 * @param referenceToDigest a mapping from the image's name to its digest
	 * @throws NullPointerException     if any of the arguments, including map keys or values, are null
	 * @throws IllegalArgumentException if the map keys or values contain whitespace or are empty
	 */
	public ImageState(Docker client, String id, Map<String, Set<String>> referenceToTags,
		Map<String, String> referenceToDigest)
	{
		assert client != null;
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();
		validateReferenceParameters(referenceToTags, referenceToDigest);
		this.client = client;
		this.id = id;

		// Create immutable copies
		Map<String, Set<String>> nameToImmutableTags = new HashMap<>();
		for (Entry<String, Set<String>> entry : referenceToTags.entrySet())
			nameToImmutableTags.put(entry.getKey(), Set.copyOf(entry.getValue()));
		this.referenceToTags = Map.copyOf(nameToImmutableTags);
		this.referenceToDigest = Map.copyOf(referenceToDigest);
	}

	/**
	 * Validates an image's reference-related parameters.
	 *
	 * @param referenceToTags   a mapping from the image's reference to its tags
	 * @param referenceToDigest a mapping from the image's reference to its digest
	 * @throws NullPointerException     if the map keys or values are null
	 * @throws IllegalArgumentException if the map keys or values contain whitespace or are empty
	 */
	static void validateReferenceParameters(Map<String, Set<String>> referenceToTags,
		Map<String, String> referenceToDigest)
	{
		for (Entry<String, Set<String>> entry : referenceToTags.entrySet())
		{
			requireThat(entry.getKey(), "reference").withContext(referenceToTags, "referenceToTags").
				doesNotContainWhitespace().isNotEmpty();
			for (String tag : entry.getValue())
			{
				requireThat(tag, "tag").withContext(referenceToTags, "referenceToTags").doesNotContainWhitespace().
					isNotEmpty();
			}
		}
		for (Entry<String, String> entry : referenceToDigest.entrySet())
		{
			requireThat(entry.getKey(), "reference").withContext(referenceToTags, "referenceToDigest").
				doesNotContainWhitespace().isNotEmpty();
			requireThat(entry.getValue(), "digest").withContext(referenceToDigest, "referenceToDigest").
				doesNotContainWhitespace().isNotEmpty();
		}
	}

	/**
	 * Returns the image's ID.
	 *
	 * @return the ID
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Returns a mapping of an image's name to its associated tags.
	 * <p>
	 * Locally, an image might have a name such as {@code nasa/rocket-ship} with tags {@code {"1.0", "latest"}},
	 * all referring to the same revision. In a registry, the same image could have a fully qualified name like
	 * {@code docker.io/nasa/rocket-ship} and be associated with multiple tags, such as
	 * {@code {"1.0", "2.0", "latest"}}, all referring to the same revision.
	 *
	 * @return an empty map if the image has no tags
	 */
	public Map<String, Set<String>> referenceToTags()
	{
		return referenceToTags;
	}

	/**
	 * Returns a mapping of an image's name on registries to its associated digest.
	 * <p>
	 * For example, an image might have a name such as {@code docker.io/nasa/rocket-ship} with digest
	 * {@code "sha256:afcc7f1ac1b49db317a7196c902e61c6c3c4607d63599ee1a82d702d249a0ccb"}.
	 *
	 * @return an empty map if the image has not been pushed to any repositories
	 */
	public Map<String, String> referenceToDigest()
	{
		return referenceToDigest;
	}

	/**
	 * Reloads the network's state.
	 *
	 * @return the updated state
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@CheckReturnValue
	public ImageState reload() throws IOException, InterruptedException
	{
		return client.getImageState(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(ImageState.class).
			add("id", id).
			add("referenceToTag", referenceToTags).
			add("referenceToDigest", referenceToDigest).
			toString();
	}
}