package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.core.id.StringId;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A docker network.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public interface Network
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the network's ID
	 * @return the type-safe identifier for the resource
	 * @throws NullPointerException     if {@code value} is null
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(String value)
	{
		return new Id(value);
	}

	/**
	 * Returns the ID of the network.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * Returns the name of the network.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the network's configurations.
	 *
	 * @return the configurations
	 */
	List<Configuration> getConfigurations();

	/**
	 * Reloads the network.
	 *
	 * @return the updated network
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@CheckReturnValue
	Network reload() throws IOException, InterruptedException;

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value the network's ID
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
		 */
		private Id(String value)
		{
			super(value);
		}
	}

	/**
	 * A network configuration.
	 *
	 * @param subnet  the network's subnet CIDR
	 * @param gateway the network's gateway
	 */
	public record Configuration(String subnet, String gateway)
	{
		/**
		 * Creates a configuration.
		 *
		 * @param subnet  the network's subnet CIDR
		 * @param gateway the network's gateway
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if any of the arguments contain whitespace or is empty
		 */
		public Configuration
		{
			requireThat(subnet, "subnet").doesNotContainWhitespace().isNotEmpty();
			requireThat(gateway, "gateway").doesNotContainWhitespace().isNotEmpty();
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder().
				add("subnet", subnet).
				add("gateway", gateway).
				toString();
		}
	}
}