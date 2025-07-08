package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultNode;

import java.io.IOException;

/**
 * Removes a swarm node.
 */
public interface NodeRemover
{
	/**
	 * Indicates that the node should be removed even if it is inaccessible, has been compromised or is not
	 * behaving as expected.
	 *
	 * @return this
	 */
	NodeRemover force();

	/**
	 * Removes a node. If the node does not exist, this method has no effect.
	 *
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws ResourceInUseException   if the node status is not {@link DefaultNode.Status#DOWN} and
	 *                                  {@link #force()} was not used, or if an attempt was made to remove the
	 *                                  current node
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	void apply() throws IOException, InterruptedException;
}