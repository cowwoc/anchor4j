package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.resource.Context;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * Default implementation of {@code Context}.
 */
public final class DefaultContext implements Context
{
	private final String name;

	/**
	 * Creates a reference to a context.
	 *
	 * @param name the context's name
	 */
	public DefaultContext(String name)
	{
		assert that(name, "name").doesNotContainWhitespace().isNotEmpty().elseThrow();
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DefaultContext other && other.name.equals(name);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DockerImage.class).
			add("name", name).
			toString();
	}
}