package io.github.cowwoc.anchor4j.container.core.internal.resource;

import io.github.cowwoc.anchor4j.container.core.client.ContainerClient;
import io.github.cowwoc.anchor4j.container.core.resource.Builder;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultBuilder implements Builder
{
	private final ContainerClient client;
	private final Id id;
	private final List<Node> nodes;
	private final Driver driver;

	/**
	 * Creates a BuilderState.
	 *
	 * @param client the client configuration
	 * @param id     builder's ID
	 * @param nodes  the nodes that the builder is on
	 * @param driver the builder's driver
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code id}'s format is invalid
	 */
	public DefaultBuilder(ContainerClient client, Id id, List<Node> nodes, Driver driver)
	{
		requireThat(client, "client").isNotNull();
		requireThat(nodes, "nodes").isNotNull();

		this.client = client;
		this.id = id;
		this.nodes = List.copyOf(nodes);
		this.driver = driver;
	}

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public String getName()
	{
		return id.getValue();
	}

	@Override
	public List<Node> getNodes()
	{
		return nodes;
	}

	@Override
	public Driver getDriver()
	{
		return driver;
	}

	@Override
	@CheckReturnValue
	public Builder reload() throws IOException, InterruptedException
	{
		return client.getBuilder(id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, nodes);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Builder other && other.getId().equals(id) && other.getNodes().equals(nodes);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder().
			add("id", id).
			add("nodes", nodes).
			toString();
	}

	public static final class DefaultNode implements Node
	{
		private final String name;
		private final Status status;
		private final String error;

		/**
		 * @param name   the name of the node
		 * @param status the status of the builder on the node
		 * @param error  an explanation of the builder's error status, or an empty string if absent
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code name} contains whitespace or is empty
		 */
		public DefaultNode(String name, Status status, String error)
		{
			requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();
			requireThat(status, "status").isNotNull();
			requireThat(error, "error").isNotNull();

			this.name = name;
			this.status = status;
			this.error = error;
		}

		@Override
		public Id getId()
		{
			return Node.id(name);
		}

		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public Status getStatus()
		{
			return status;
		}

		@Override
		public String getError()
		{
			return error;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(name, status, error);
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof Node other && other.getName().equals(name) && other.getStatus().equals(status) &&
				other.getError().equals(error);
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(DefaultNode.class).
				add("name", name).
				add("status", status).
				add("error", error).
				toString();
		}
	}
}