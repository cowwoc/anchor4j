package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.container.core.internal.client.InternalContainerClient;
import io.github.cowwoc.anchor4j.container.core.internal.resource.DefaultContainerImageBuilder;
import io.github.cowwoc.anchor4j.container.core.resource.BuildListener;
import io.github.cowwoc.anchor4j.container.core.resource.Builder;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.DockerImageBuilder;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents an operation that builds an image.
 */
public final class DefaultDockerImageBuilder extends DefaultContainerImageBuilder
	implements DockerImageBuilder
{
	/**
	 * Creates a DefaultDockerImageBuilder.
	 *
	 * @param client the client configuration
	 */
	public DefaultDockerImageBuilder(InternalContainerClient client)
	{
		super(client);
	}

	@Override
	public DockerImageBuilder dockerfile(Path dockerfile)
	{
		return (DockerImageBuilder) super.dockerfile(dockerfile);
	}

	@Override
	public DockerImageBuilder platform(String platform)
	{
		return (DockerImageBuilder) super.platform(platform);
	}

	@Override
	public DockerImageBuilder reference(String reference)
	{
		return (DockerImageBuilder) super.reference(reference);
	}

	@Override
	public DockerImageBuilder cacheFrom(String source)
	{
		return (DockerImageBuilder) super.cacheFrom(source);
	}

	@Override
	public DockerImageBuilder export(Exporter exporter)
	{
		return (DockerImageBuilder) super.export(exporter);
	}

	@Override
	public DockerImageBuilder builder(Builder.Id builder)
	{
		return (DockerImageBuilder) super.builder(builder);
	}

	@Override
	public DockerImageBuilder listener(BuildListener listener)
	{
		return (DockerImageBuilder) super.listener(listener);
	}

	@Override
	public DockerImage apply(String buildContext) throws IOException, InterruptedException
	{
		return (DockerImage) super.apply(buildContext);
	}

	@Override
	public DockerImage apply(Path buildContext) throws IOException, InterruptedException
	{
		return (DockerImage) super.apply(buildContext);
	}
}