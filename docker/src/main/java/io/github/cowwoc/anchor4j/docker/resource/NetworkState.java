package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A snapshot of a network's state.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public final class NetworkState
{
	private final Docker client;
	private final String id;
	private final String name;
	private final List<Configuration> configurations;

	/**
	 * Creates a state.
	 *
	 * @param client         the client configuration
	 * @param id             the ID of the network
	 * @param name           the name of the network
	 * @param configurations the network configurations
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code id} or {@code name} contain whitespace or are empty
	 */
	public NetworkState(Docker client, String id, String name, List<Configuration> configurations)
	{
		requireThat(client, "client").isNotNull();
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();

		this.client = client;
		this.id = id;
		this.name = name;
		this.configurations = List.copyOf(configurations);
	}

	/**
	 * Returns the ID of the network.
	 *
	 * @return the ID
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Returns the name of the network.
	 *
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the network's configurations.
	 *
	 * @return the configurations
	 */
	public List<Configuration> getConfigurations()
	{
		return configurations;
	}

	/**
	 * Reloads the network's state.
	 *
	 * @return the updated state
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@CheckReturnValue
	public NetworkState reload() throws IOException, InterruptedException
	{
		return client.getNetworkState(id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, configurations);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof NetworkState other && other.id.equals(id) && other.name.equals(name) &&
			other.configurations.equals(configurations);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder().
			add("id", id).
			add("name", name).
			add("configurations", configurations).
			toString();
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