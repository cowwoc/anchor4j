package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.internal.util.Buffers;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

import static io.github.cowwoc.anchor4j.docker.internal.resource.DefaultConfigCreator.NAME_PATTERN;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The state of a swarm's config.
 * <p>
 * <b>Thread Safety</b>: This class is immutable and thread-safe.
 */
public final class ConfigState
{
	private final Docker client;
	private final String id;
	private final String name;
	private final ByteBuffer value;

	/**
	 * Creates a config state.
	 *
	 * @param client the client configuration
	 * @param id     the config's ID
	 * @param name   the config's name
	 * @param value  the config's value
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code id} or {@code name} contain whitespace or are empty
	 */
	public ConfigState(Docker client, String id, String name, ByteBuffer value)
	{
		requireThat(client, "client").isNotNull();
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();
		requireThat(name, "name").doesNotContainWhitespace().matches(NAME_PATTERN);
		requireThat(value, "value").isNotNull();
		requireThat(value.remaining(), "value.remaining()").isLessThanOrEqualTo(ConfigCreator.MAX_SIZE_IN_BYTES);
		this.client = client;
		this.id = id;
		this.name = name;
		this.value = Buffers.copyOf(value);
	}

	/**
	 * Returns the config's id.
	 *
	 * @return the config's id
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Returns the config's name.
	 *
	 * @return the config's name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns config's value.
	 *
	 * @return the value
	 */
	public ByteBuffer getValue()
	{
		return value;
	}

	/**
	 * Returns the String representation of the config's value.
	 *
	 * @return the value
	 */
	public String getValueAsString()
	{
		return UTF_8.decode(value.duplicate()).toString();
	}

	/**
	 * Reloads the config's state.
	 *
	 * @return the updated state
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	@CheckReturnValue
	public ConfigState reload() throws IOException, InterruptedException
	{
		return client.getConfigState(id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, value);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof ConfigState other && other.id.equals(id) && other.name.equals(name) &&
			other.value.equals(value);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(ConfigState.class).
			add("id", id).
			add("name", name).
			add("value", getValueAsString()).
			toString();
	}
}