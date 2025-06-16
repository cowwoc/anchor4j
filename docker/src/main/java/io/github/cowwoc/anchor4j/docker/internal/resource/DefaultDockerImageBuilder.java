package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.client.InternalClient;
import io.github.cowwoc.anchor4j.core.internal.resource.DefaultCoreImageBuilder;
import io.github.cowwoc.anchor4j.core.resource.BuildListener;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.DockerImageBuilder;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents an operation that builds an image.
 */
public final class DefaultDockerImageBuilder extends DefaultCoreImageBuilder
	implements DockerImageBuilder
{
	/**
	 * Creates a DefaultDockerImageBuilder.
	 *
	 * @param client the client configuration
	 */
	public DefaultDockerImageBuilder(InternalClient client)
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
	public DockerImageBuilder builder(String builder)
	{
		return (DockerImageBuilder) super.builder(builder);
	}

	@Override
	public DockerImageBuilder listener(BuildListener listener)
	{
		return (DockerImageBuilder) super.listener(listener);
	}

	@Override
	public DockerImage build(String buildContext) throws IOException, InterruptedException
	{
		return (DockerImage) super.build(buildContext);
	}

	@Override
	public DockerImage build(Path buildContext) throws IOException, InterruptedException
	{
		return (DockerImage) super.build(buildContext);
	}
}