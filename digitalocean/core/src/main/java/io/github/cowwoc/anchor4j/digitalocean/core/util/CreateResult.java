package io.github.cowwoc.anchor4j.digitalocean.core.util;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;

/**
 * The result of trying to create a resource.
 *
 * @param <T> the type of resource that is being created
 */
public final class CreateResult<T>
{
	/**
	 * Indicates that the operation succeeded.
	 *
	 * @param <T>      the type of the resource
	 * @param resource the created resource
	 * @return the result of the create operation
	 */
	public static <T> CreateResult<T> created(T resource)
	{
		return new CreateResult<>(true, resource);
	}

	/**
	 * Indicates that the operation failed due to a conflicting resource.
	 *
	 * @param <T>      the type of the resource
	 * @param resource the conflicting resource
	 * @return the result of the create operation
	 */
	public static <T> CreateResult<T> conflictedWith(T resource)
	{
		return new CreateResult<>(false, resource);
	}

	private final boolean created;
	private final T resource;

	/**
	 * @param resource the returned resource
	 * @param created  {@code true} if the operation succeeded or {@code false} if the operation failed due to a
	 *                 conflicting resource
	 */
	private CreateResult(boolean created, T resource)
	{
		assert resource != null;
		this.resource = resource;
		this.created = created;
	}

	/**
	 * Determines if the operation successfully created a new resource.
	 *
	 * @return {@code true} if a new resource was created or {@code false} if a conflicting resource existed
	 */
	public boolean created()
	{
		return created;
	}

	/**
	 * Determines if the operation encountered a conflict with an existing resource.
	 *
	 * @return {@code true} if a conflicting resource existed or {@code false} if a new resource was created
	 */
	public boolean conflicted()
	{
		return !created;
	}

	/**
	 * Returns the created or conflicting resource.
	 *
	 * @return the created resource if {@link #created()} returns {@code true} or the conflicting resource if it
	 * 	returns {@code false}
	 */
	public T getResource()
	{
		return resource;
	}

	@Override
	public String toString()
	{
		String type;
		if (created)
			type = "Created";
		else
			type = "Conflicting";
		return new ToStringBuilder(CreateResult.class).
			add(type, resource).
			toString();
	}
}