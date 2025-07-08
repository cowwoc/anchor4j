package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;

/**
 * A collection of Docker containers.
 */
public interface Swarm
{
	/**
	 * Leaves the swarm.
	 *
	 * @return a swarm leaver
	 */
	@CheckReturnValue
	SwarmLeaver leave();

	/**
	 * Returns the secret value needed to join the swarm as a manager.
	 *
	 * @return the join token
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	JoinToken getManagerJoinToken() throws IOException, InterruptedException;

	/**
	 * Returns the secret value needed to join the swarm as a worker.
	 *
	 * @return the join token
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	JoinToken getWorkerJoinToken() throws IOException, InterruptedException;
}