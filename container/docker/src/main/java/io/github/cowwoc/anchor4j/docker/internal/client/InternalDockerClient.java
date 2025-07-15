package io.github.cowwoc.anchor4j.docker.internal.client;

import io.github.cowwoc.anchor4j.container.core.internal.client.InternalContainerClient;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.internal.parser.ConfigParser;
import io.github.cowwoc.anchor4j.docker.internal.parser.ContainerParser;
import io.github.cowwoc.anchor4j.docker.internal.parser.ContextParser;
import io.github.cowwoc.anchor4j.docker.internal.parser.ImageParser;
import io.github.cowwoc.anchor4j.docker.internal.parser.NetworkParser;
import io.github.cowwoc.anchor4j.docker.internal.parser.NodeParser;
import io.github.cowwoc.anchor4j.docker.internal.parser.ServiceParser;
import io.github.cowwoc.anchor4j.docker.internal.parser.SwarmParser;

/**
 * The internals of a Docker client.
 */
public interface InternalDockerClient extends InternalContainerClient, DockerClient
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