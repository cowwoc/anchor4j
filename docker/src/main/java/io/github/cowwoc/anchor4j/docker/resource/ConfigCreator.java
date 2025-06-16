package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Creates a {@code Config}.
 */
public interface ConfigCreator
{
	/**
	 * The maximum size of a config value in bytes.
	 */
	int MAX_SIZE_IN_BYTES = 1000 * 1024;

	/**
	 * Adds a key-value metadata pair that provide additional information about the Config, such as environment
	 * or usage context (e.g., {@code environment=production}).
	 *
	 * @param name  the name of the label
	 * @param value the value of the label
	 * @return this
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if any of the arguments contain whitespace or are empty
	 */
	ConfigCreator label(String name, String value);

	/**
	 * Creates a config containing a {@link StandardCharsets#UTF_8 UTF_8}-encoded String.
	 *
	 * @param name  the config's name
	 * @param value the config's value
	 * @return the config
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code name} is empty.</li>
	 *                                    <li>{@code name} contains more than 64 characters.</li>
	 *                                    <li>{@code name} contains characters other than
	 *                                    {@code [a-zA-Z0-9-_.]}.</li>
	 *                                    <li>another configuration with the same {@code name} already
	 *                                    exists.</li>
	 *                                    <li>{@code value.getBytes(UTF_8).length} is greater than
	 *                                    {@link #MAX_SIZE_IN_BYTES}.</li>
	 *                                  </ul>
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws ResourceInUseException   if the requested name is in use by another config
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	Config create(String name, String value) throws IOException, InterruptedException;

	/**
	 * Creates a config.
	 *
	 * @param name  the config's name
	 * @param value the config's value
	 * @return the config
	 * @throws NullPointerException     if {@code name} or {@code value} are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code name} is empty.</li>
	 *                                    <li>{@code name} contains more than 64 characters.</li>
	 *                                    <li>{@code name} contains characters other than
	 *                                    {@code [a-zA-Z0-9-_.]}.</li>
	 *                                    <li>another configuration with the same {@code name} already
	 *                                    exists.</li>
	 *                                    <li>{@code value} contains more than {@link #MAX_SIZE_IN_BYTES}
	 *                                    bytes.</li>
	 *                                  </ul>
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws ResourceInUseException   if the requested name is in use by another config
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	Config create(String name, ByteBuffer value) throws IOException, InterruptedException;
}