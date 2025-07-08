package io.github.cowwoc.anchor4j.container.core.internal.resource;

import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public class DefaultContainerImage implements ContainerImage
{
	protected final Id id;

	/**
	 * Creates a reference to an image.
	 *
	 * @param id the image's ID or reference
	 * @throws NullPointerException if {@code id} is null
	 */
	public DefaultContainerImage(Id id)
	{
		requireThat(id, "id").isNotNull();
		this.id = id;
	}

	@Override
	public Id getId()
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
		return o instanceof ContainerImage other && other.getId().equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultContainerImage.class).
			add("id", id).
			toString();
	}
}