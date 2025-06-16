package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;

/**
 * A docker container, which is a running instance of an image.
 * <p>
 * <b>Thread Safety</b>: This class is immutable and thread-safe.
 */
public interface Container
{
	/**
	 * Returns the container's ID.
	 *
	 * @return the ID
	 */
	String getId();

	/**
	 * Looks up the container's state.
	 *
	 * @return null if the container does not exist
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	ContainerState getState() throws IOException, InterruptedException;

	/**
	 * Renames the container.
	 *
	 * @param newName the container's new name
	 * @return this
	 * @throws IllegalArgumentException  if {@code newName}:
	 *                                   <ul>
	 *                                     <li>is empty.</li>
	 *                                     <li>contains any character other than lowercase letters (a–z),
	 *                                     digits (0–9), and the following characters: {@code '.'}, {@code '/'},
	 *                                     {@code ':'}, {@code '_'}, {@code '-'}, {@code '@'}.</li>
	 *                                   </ul>
	 * @throws ResourceNotFoundException if the container does not exist
	 * @throws ResourceInUseException    if the requested name is in use by another container
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	Container rename(String newName) throws IOException, InterruptedException;

	/**
	 * Starts the container.
	 *
	 * @return a container starter
	 */
	@CheckReturnValue
	ContainerStarter starter();

	/**
	 * Stops the container.
	 *
	 * @return a container stopper
	 */
	@CheckReturnValue
	ContainerStopper stopper();

	/**
	 * Removes the container.
	 *
	 * @return a container remover
	 */
	@CheckReturnValue
	ContainerRemover remover();

	/**
	 * Waits until the container stops.
	 * <p>
	 * If the container has already stopped, this method returns immediately.
	 *
	 * @return the exit code returned by the container
	 * @throws ResourceNotFoundException if the container does not exist
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	int waitUntilStop() throws IOException, InterruptedException;

	/**
	 * Retrieves the container's logs.
	 *
	 * @return the logs
	 */
	ContainerLogs getLogs();
}