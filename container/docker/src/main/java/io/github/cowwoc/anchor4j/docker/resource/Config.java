package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.id.StringId;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents non-sensitive configuration stored in a Swarm.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public interface Config
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the config's name
	 * @return the type-safe identifier for the resource
	 * @throws NullPointerException     if {@code value} is null
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(String value)
	{
		return new Id(value);
	}

	/**
	 * Returns the config's ID.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * Returns the config's name.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns config's value.
	 *
	 * @return the value
	 */
	ByteBuffer getValue();

	/**
	 * Returns the String representation of the config's value.
	 *
	 * @return the value
	 */
	String getValueAsString();

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
	Config reload() throws IOException, InterruptedException;

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value the config's name
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value}'s format is invalid
		 */
		private Id(String value)
		{
			super(value);
			ParameterValidator.validateName(value, "value");
		}
	}
}