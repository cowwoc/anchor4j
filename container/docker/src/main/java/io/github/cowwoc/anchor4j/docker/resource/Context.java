package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.id.StringId;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;

/**
 * Represents a Docker context (i.e., the Docker Engine that the client communicates with).
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 *
 * @see <a href="https://docs.docker.com/engine/manage-resources/contexts/">Docker documentation</a>
 */
public interface Context
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the context's name
	 * @return the type-safe identifier for the resource
	 * @throws NullPointerException     if {@code value} is null
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(String value)
	{
		if (value.isEmpty())
			return null;
		return new Id(value);
	}

	/**
	 * Returns the context's ID.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * Returns the context's name.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the context's description.
	 *
	 * @return an empty string if omitted
	 */
	String getDescription();

	/**
	 * Reloads the context's state.
	 *
	 * @return the updated state
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@CheckReturnValue
	Context reload() throws IOException, InterruptedException;

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value the context's name
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