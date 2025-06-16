package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.resource.Network;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * The default implementation of {@code Network}.
 */
public final class DefaultNetwork implements Network
{
	private final String id;

	/**
	 * Creates a network.
	 *
	 * @param id the ID of the network
	 */
	public DefaultNetwork(String id)
	{
		assert that(id, "id").doesNotContainWhitespace().isNotEmpty().elseThrow();
		this.id = id;
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DefaultNetwork other && other.id.equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder().
			add("id", id).
			toString();
	}
}