package io.github.cowwoc.anchor4j.core.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.client.InternalClient;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.Builder;
import io.github.cowwoc.anchor4j.core.resource.BuilderState;

import java.io.IOException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

public final class DefaultBuilder implements Builder
{
	private final InternalClient client;
	private final String name;

	/**
	 * Creates a reference to a builder.
	 *
	 * @param client the client configuration
	 * @param name   the builder's name
	 */
	public DefaultBuilder(InternalClient client, String name)
	{
		assert client != null;
		assert that(name, "name").doesNotContainWhitespace().isNotEmpty().elseThrow();
		this.client = client;
		this.name = name;
	}

	/**
	 * Returns the builder's name.
	 *
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Looks up the builder's state.
	 *
	 * @return null if the builder does not exist
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	public BuilderState getState() throws IOException, InterruptedException
	{
		return client.getBuilderState(name);
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DefaultBuilder other && other.name.equals(name);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultBuilder.class).
			add("name", name).
			toString();
	}
}