package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;

import java.io.IOException;

/**
 * Leaves a Swarm.
 */
public interface SwarmLeaver
{
	/**
	 * Indicates that the context should be removed even if it is in use by a client.
	 *
	 * @return this
	 */
	SwarmLeaver force();

	/**
	 * Leaves the swarm. If the node is not a member of a swarm, this method has no effect.
	 *
	 * @throws ResourceInUseException if the node is a manager and {@link #force()} was not used. The safe way
	 *                                to remove a manager from a swarm is to demote it to a worker and then
	 *                                direct it to leave the quorum without using {@code force}. Only use
	 *                                {@code force} in situations where the swarm will no longer be used after
	 *                                the manager leaves, such as in a single-node swarm.
	 * @throws IOException            if an I/O error occurs. These errors are typically transient, and retrying
	 *                                the request may resolve the issue.
	 * @throws InterruptedException   if the thread is interrupted before the operation completes. This can
	 *                                happen due to shutdown signals.
	 */
	void leave() throws IOException, InterruptedException;
}