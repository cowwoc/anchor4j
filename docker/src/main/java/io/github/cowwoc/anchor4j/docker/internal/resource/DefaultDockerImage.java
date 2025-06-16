package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.resource.DefaultCoreImage;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.ContainerCreator;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.ImagePuller;
import io.github.cowwoc.anchor4j.docker.resource.ImagePusher;
import io.github.cowwoc.anchor4j.docker.resource.ImageRemover;
import io.github.cowwoc.anchor4j.docker.resource.ImageState;

import java.io.IOException;

/**
 * The default implementation of {@code DockerImage}.
 */
public final class DefaultDockerImage extends DefaultCoreImage
	implements DockerImage
{
	private final InternalDocker client;

	/**
	 * Creates a reference to an image.
	 *
	 * @param client the client configuration
	 * @param id     the image's ID or reference
	 */
	public DefaultDockerImage(InternalDocker client, String id)
	{
		super(id);
		assert client != null;
		this.client = client;
	}

	@Override
	public ImageState getState() throws IOException, InterruptedException
	{
		return client.getImageState(id);
	}

	@Override
	public ContainerCreator createContainer()
	{
		return client.createContainer(id);
	}

	@Override
	public DockerImage tag(String target) throws IOException, InterruptedException
	{
		client.tagImage(id, target);
		return this;
	}

	@Override
	public ImagePuller puller()
	{
		return client.pullImage(id);
	}

	@Override
	public ImagePusher pusher() throws IOException, InterruptedException
	{
		return client.pushImage(id);
	}

	@Override
	public ImageRemover remover()
	{
		return client.removeImage(id);
	}
}