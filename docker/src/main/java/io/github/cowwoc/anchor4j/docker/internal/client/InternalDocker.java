package io.github.cowwoc.anchor4j.docker.internal.client;

import io.github.cowwoc.anchor4j.core.internal.client.InternalClient;
import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.anchor4j.docker.internal.resource.ConfigParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.ContainerParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.ContextParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.ImageParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.NetworkParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.NodeParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.ServiceParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.SwarmParser;

/**
 * The internals of a Docker client.
 */
public interface InternalDocker extends InternalClient, Docker
{
	/**
	 * @return a {@code ContainerParser}
	 */
	ContainerParser getContainerParser();

	/**
	 * @return a {@code ConfigParser}
	 */
	ConfigParser getConfigParser();

	/**
	 * @return a {@code ImageParser}
	 */
	ImageParser getImageParser();

	/**
	 * @return a {@code ContextParser}
	 */
	ContextParser getContextParser();

	/**
	 * @return a {@code NetworkParser}
	 */
	NetworkParser getNetworkParser();

	/**
	 * @return a {@code ServiceParser}
	 */
	ServiceParser getServiceParser();

	/**
	 * @return a {@code NodeParser}
	 */
	NodeParser getNodeParser();

	/**
	 * @return a {@code SwarmParser}
	 */
	SwarmParser getSwarmParser();
}