package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.ImagePuller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Default implementation of {@code ImagePuller}.
 */
public final class DefaultImagePuller implements ImagePuller
{
	private final InternalDocker client;
	private final String reference;
	private String platform = "";

	/**
	 * Creates an image puller.
	 *
	 * @param client    the client configuration
	 * @param reference the image's reference
	 * @throws NullPointerException     if {@code reference} is null
	 * @throws IllegalArgumentException if {@code reference}:
	 *                                  <ul>
	 *                                    <li>is empty.</li>
	 *                                    <li>contains any character other than lowercase letters (a–z),
	 *                                    digits (0–9), and the following characters: {@code '.'}, {@code '/'},
	 *                                    {@code ':'}, {@code '_'}, {@code '-'}, {@code '@'}.</li>
	 *                                  </ul>
	 */
	public DefaultImagePuller(InternalDocker client, String reference)
	{
		assert client != null;
		ParameterValidator.validateImageReference(reference, "reference");
		this.client = client;
		this.reference = reference;
	}

	@Override
	public ImagePuller platform(String platform)
	{
		requireThat(platform, "platform").doesNotContainWhitespace().isNotEmpty();
		this.platform = platform;
		return this;
	}

	@Override
	public DockerImage pull() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/image/pull/
		List<String> arguments = new ArrayList<>(5);
		arguments.add("image");
		arguments.add("pull");
		if (!platform.isEmpty())
		{
			arguments.add("platform");
			arguments.add(platform);
		}
		arguments.add(reference);
		CommandResult result = client.retry(deadline -> client.run(arguments, deadline));
		return client.image(client.getImageParser().pull(result, reference));
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultImagePuller.class).
			add("reference", reference).
			add("platform", platform).
			toString();
	}
}