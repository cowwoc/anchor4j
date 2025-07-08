package io.github.cowwoc.anchor4j.container.core.resource;

import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.id.StringId;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;

/**
 * Represents a service that builds images.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public interface Builder
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the server-side identifier
	 * @return the type-safe identifier for the resource
	 * @throws NullPointerException     if {@code value} is null
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(String value)
	{
		return new Id(value);
	}

	/**
	 * Returns the build's ID.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * Returns the name of the builder.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the status of the builder.
	 *
	 * @return the status
	 */
	Status getStatus();

	/**
	 * Returns an explanation of the builder's error status.
	 *
	 * @return an empty string if the status is not {@code ERROR}
	 */
	String getError();

	/**
	 * Reloads the builder.
	 *
	 * @return the updated builder
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@CheckReturnValue
	Builder reload() throws IOException, InterruptedException;

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value the name of the builder. The value must start with a letter, or digit, or underscore, and
		 *              may be followed by additional characters consisting of letters, digits, underscores,
		 *              periods or hyphens.
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value}'s format is invalid
		 */
		private Id(String value)
		{
			super(value);
			ParameterValidator.validateName(value, "value");
		}
	}

	/**
	 * Represents the status of a builder.
	 * <p>
	 * <b>Thread Safety</b>: This class is immutable and thread-safe.
	 */
	enum Status
	{
		/**
		 * The builder is defined but has not been created yet.
		 * <p>
		 * For example, this status can occur before the BuildKit image has been pulled locally and the builder
		 * instance needs to be initialized.
		 */
		INACTIVE,
		/**
		 * The builder is in the process of starting up. Resources are being initialized, but it is not yet ready
		 * to accept build jobs.
		 */
		STARTING,
		/**
		 * The builder is up and ready to accept jobs.
		 */
		RUNNING,
		/**
		 * The builder is in the process of shutting down. Active jobs may still be completing.
		 */
		STOPPING,
		/**
		 * The builder exists but is not currently running.
		 */
		STOPPED,
		/**
		 * The builder is unavailable due to an error.
		 */
		ERROR
	}
}