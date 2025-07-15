package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.container.core.resource.BuildListener;
import io.github.cowwoc.anchor4j.container.core.resource.Builder;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImageBuilder;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents an operation that builds an image using Docker.
 */
public interface DockerImageBuilder extends ContainerImageBuilder
{
	@Override
	DockerImageBuilder dockerfile(Path dockerfile);

	@Override
	DockerImageBuilder platform(String platform);

	@Override
	DockerImageBuilder reference(String reference);

	@Override
	DockerImageBuilder cacheFrom(String source);

	@Override
	DockerImageBuilder export(Exporter exporter);

	@Override
	DockerImageBuilder builder(Builder.Id builder);

	@Override
	DockerImageBuilder listener(BuildListener listener);

	@Override
	DockerImage apply(String buildContext) throws IOException, InterruptedException;

	@Override
	DockerImage apply(Path buildContext) throws IOException, InterruptedException;
}