package io.github.cowwoc.anchor4j.docker.resource;

import java.io.BufferedReader;
import java.io.InputStream;

/**
 * A class that observes and reacts to an external process.
 * <p>
 * Implementations must support repeated invocations of the process, as retries may occur due to intermittent
 * failures.
 */
public interface ProcessListener
{
	/**
	 * Returns the container's standard output as a byte stream.
	 *
	 * @return the output log
	 */
	InputStream getOutputStream();

	/**
	 * Returns the container's standard output as a {@code BufferedReader}, using the host's default character
	 * encoding.
	 *
	 * @return the output log
	 */
	BufferedReader getOutputReader();

	/**
	 * Returns the container's standard error as a byte stream.
	 *
	 * @return the error log
	 */
	InputStream getErrorStream();

	/**
	 * Returns the container's standard error as a {@code BufferedReader}, using the host's default character
	 * encoding.
	 *
	 * @return the error log
	 */
	BufferedReader getErrorReader();

	/**
	 * Blocks until the operation completes.
	 *
	 * @return the exit code returned by {@code docker logs}
	 * @throws InterruptedException if the thread is interrupted before the operation completes
	 */
	int waitFor() throws InterruptedException;
}