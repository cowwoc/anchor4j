package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;

import java.io.IOException;
import java.time.Duration;

/**
 * Stops a container.
 */
public interface ContainerStopper
{
	/**
	 * Sets the signal to send to the container.
	 *
	 * @param signal the signal to send to the container. Common values include {@code SIGTERM},
	 *               {@code SIGKILL}, {@code SIGHUP}, and {@code SIGINT}. If an empty string is provided, the
	 *               default signal will be used.
	 *               <p>
	 *               The default signal is determined by the container's configuration. It can be set using the
	 *               <a href="https://docs.docker.com/reference/dockerfile/#stopsignal">STOPSIGNAL</a>
	 *               instruction in the Dockerfile, or via the {@code stopSignal} option when creating the
	 *               container. If no default is specified, {@code SIGTERM} is used.
	 * @return this
	 * @throws NullPointerException     if {@code signal} is null
	 * @throws IllegalArgumentException if {@code signal} contains whitespace
	 * @see <a href="https://man7.org/linux/man-pages/man7/signal.7.html">the list of available signals</a>
	 */
	ContainerStopper signal(String signal);

	/**
	 * Sets the maximum duration to wait for the container to stop.
	 *
	 * @param timeout the maximum duration to wait for the container to stop after sending the specified
	 *                {@code signal}. If the container does not exit within this time, it will be forcibly
	 *                terminated with a {@code SIGKILL}.
	 *                <p>
	 *                If negative, the method will wait indefinitely for the container to exit.
	 *                <p>
	 *                If {@code null}, the default timeout will be used.
	 *                <p>
	 *                The default timeout can be configured using the {@code stopTimeout} option when the
	 *                container is created. If no default is configured for the container, the value defaults to
	 *                10 seconds for Linux containers and 30 seconds for Windows containers.
	 * @return this
	 */
	ContainerStopper timeout(Duration timeout);

	/**
	 * Stops a container. If the container is already stopped, this method has no effect.
	 *
	 * @return the container
	 * @throws ResourceNotFoundException if the container no longer exists
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	Container apply() throws IOException, InterruptedException;
}