package io.github.cowwoc.anchor4j.core.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CoreImage;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * Default implementation of {@code CoreImage}.
 */
public class DefaultCoreImage implements CoreImage
{
	protected final String id;

	/**
	 * Creates a reference to an image.
	 *
	 * @param id the image's ID or reference
	 */
	public DefaultCoreImage(String id)
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
		return o instanceof DefaultCoreImage other && other.id.equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultCoreImage.class).
			add("id", id).
			toString();
	}
}