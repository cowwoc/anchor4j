package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Starts a container.
 */
public interface ContainerStarter
{
	/**
	 * Starts a container. If the container is already started, this method has no effect.
	 *
	 * @return the container
	 * @throws ResourceNotFoundException if the image or container no longer exist
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	Container start() throws IOException, InterruptedException;

	/**
	 * Starts the container and attaches its streams and exit code. If the container is already started, this
	 * method has no effect. If the operation fails, {@code stderr} will return an error message and
	 * {@link ContainerStreams#waitFor()} will return a non-zero exit code.
	 *
	 * @param attachInput  {@code true} to attach the {@code stdin} stream
	 * @param attachOutput {@code true} to attach the {@code stdout}, {@code stderr} streams and exit code
	 * @return the streams
	 * @throws IllegalArgumentException if {@code attachInput} and {@code attachOutput} are both {@code false}
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 */
	ContainerStreams startAndAttachStreams(boolean attachInput, boolean attachOutput) throws IOException;

	/**
	 * A container's stdin, stdout, and stderr streams.
	 */
	interface ContainerStreams extends AutoCloseable
	{
		/**
		 * Returns the standard input stream of the container if
		 * {@link #startAndAttachStreams(boolean, boolean) attachInput} is {@code true} or the docker command if
		 * it is {@code false}.
		 *
		 * @return the stream
		 */
		OutputStream getStdin();

		/**
		 * Returns the standard output stream of the container if
		 * {@link #startAndAttachStreams(boolean, boolean) attachOutput} is {@code true} or the docker command if
		 * it is {@code false}.
		 *
		 * @return the stream
		 */
		InputStream getStdout();

		/**
		 * Returns the standard error stream of the container if
		 * {@link #startAndAttachStreams(boolean, boolean) attachOutput} is {@code true} or the docker command if
		 * it is {@code false}.
		 *
		 * @return the stream
		 */
		InputStream getStderr();

		/**
		 * Blocks until the operation completes.
		 *
		 * @return the exit code of the container if {@link #startAndAttachStreams(boolean, boolean) attachOutput}
		 * 	is {@code true} or the docker command if it is {@code false}
		 * @throws InterruptedException if the thread is interrupted before the operation completes
		 */
		int waitFor() throws InterruptedException;

		/**
		 * Releases the streams.
		 *
		 * @throws IOException if an I/O error occurs while closing the streams
		 */
		@Override
		void close() throws IOException;
	}
}