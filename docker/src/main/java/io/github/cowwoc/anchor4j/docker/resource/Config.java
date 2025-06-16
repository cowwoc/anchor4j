package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;

import java.io.IOException;

/**
 * Represents non-sensitive configuration stored in a Swarm.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public interface Config
{
	/**
	 * Returns the config's ID.
	 *
	 * @return the ID
	 */
	String getId();

	/**
	 * Looks up the config's state.
	 *
	 * @return null if the config does not exist
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	ConfigState getState() throws IOException, InterruptedException;
}