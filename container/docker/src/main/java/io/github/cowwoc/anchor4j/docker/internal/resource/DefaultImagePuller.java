package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
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
	private final InternalDockerClient client;
	private final String reference;
	private String platform = "";

	/**
	 * Creates an image puller.
	 *
	 * @param client    the client configuration
	 * @param reference the reference to pull. For example, {@code docker.io/nasa/rocket-ship}
	 * @throws NullPointerException     if {@code reference} is null
	 * @throws IllegalArgumentException if {@code reference}'s reference is invalid
	 */
	public DefaultImagePuller(InternalDockerClient client, String reference)
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
	public DockerImage apply() throws IOException, InterruptedException
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
		CommandResult result = client.retry(_ -> client.run(arguments));
		return client.getImage(client.getImageParser().pull(result, reference));
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