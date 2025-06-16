package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.core.resource.BuildListener;
import io.github.cowwoc.anchor4j.core.resource.CoreImageBuilder;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents an operation that builds an image using Docker.
 */
public interface DockerImageBuilder extends CoreImageBuilder
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
	DockerImageBuilder builder(String builder);

	@Override
	DockerImageBuilder listener(BuildListener listener);

	@Override
	DockerImage build(String buildContext) throws IOException, InterruptedException;

	@Override
	DockerImage build(Path buildContext) throws IOException, InterruptedException;
}