package io.github.cowwoc.anchor4j.core.resource;

import io.github.cowwoc.anchor4j.core.client.Client;
import io.github.cowwoc.anchor4j.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.Objects;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A builder's state.
 * <p>
 * <b>Thread Safety</b>: This class is immutable and thread-safe.
 */
public final class BuilderState
{
	private final Client client;
	private final String name;
	private final Status status;
	private final String error;

	/**
	 * Creates a BuilderState.
	 *
	 * @param client the client configuration
	 * @param name   the name of the builder. The value must start with a letter, or digit, or underscore, and
	 *               may be followed by additional characters consisting of letters, digits, underscores,
	 *               periods or hyphens.
	 * @param status the builder's status
	 * @param error  an explanation of the builder's error status, or an empty string if the status is not
	 *               {@code ERROR}
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code name}'s format is invalid
	 */
	public BuilderState(Client client, String name, Status status, String error)
	{
		requireThat(client, "client").isNotNull();
		ParameterValidator.validateName(name, "name");
		requireThat(status, "status").isNotNull();
		requireThat(error, "error").isNotNull();

		this.client = client;
		this.name = name;
		this.status = status;
		this.error = error;
	}

	/**
	 * Returns the name of the builder.
	 *
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the status of the builder.
	 *
	 * @return the status
	 */
	public Status getStatus()
	{
		return status;
	}

	/**
	 * Returns an explanation of the builder's error status.
	 *
	 * @return an empty string if the status is not {@code ERROR}
	 */
	public String getError()
	{
		return error;
	}

	/**
	 * Reloads the builder's state.
	 *
	 * @return the updated state
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@CheckReturnValue
	public BuilderState reload() throws IOException, InterruptedException
	{
		return client.getBuilderState(name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, status, error);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof BuilderState other && other.name.equals(name) && other.status.equals(status) &&
			other.error.equals(error);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder().
			add("name", name).
			add("status", status).
			add("error", error).
			toString();
	}

	/**
	 * Represents the status of a builder.
	 * <p>
	 * <b>Thread Safety</b>: This class is immutable and thread-safe.
	 */
	public enum Status
	{
		/**
		 * The builder is running.
		 */
		RUNNING,
		/**
		 * The builder is unavailable due to an error.
		 */
		ERROR;
	}
}