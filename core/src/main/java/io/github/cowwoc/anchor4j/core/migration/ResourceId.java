package io.github.cowwoc.anchor4j.core.migration;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Values that uniquely identify a resource's state.
 *
 * @param type the type of the resource
 * @param id   a value that identifies the resource uniquely across other resources with the same type
 */
public record ResourceId(Class<?> type, String id)
{
	/**
	 * Creates a new ResourceId.
	 *
	 * @param type the type of the resource
	 * @param id   a value that identifies the resource uniquely across other resources with the same type
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 */
	public ResourceId
	{
		requireThat(type, "type").isNotNull();
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();
	}
}