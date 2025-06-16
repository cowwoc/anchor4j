package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.resource.Task;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * The default implementation of {@code Task}.
 */
public final class DefaultTask implements Task
{
	private final String id;

	/**
	 * Creates a reference to a task.
	 *
	 * @param id the task's ID
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 */
	public DefaultTask(String id)
	{
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();
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
		return o instanceof DefaultTask other && other.id.equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultTask.class).
			add("id", id).
			toString();
	}
}