package io.github.cowwoc.anchor4j.core.id;

import java.util.Objects;

/**
 * A resource identifier of type {@code int}.
 * <p>
 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place of
 * IDs belonging to another class.
 */
public abstract class IntegerId
{
	private final int value;

	/**
	 * Creates a new ID.
	 *
	 * @param value the value of the ID
	 */
	protected IntegerId(int value)
	{
		this.value = value;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(value);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof IntegerId other && Objects.equals(other.value, value);
	}

	/**
	 * Returns the {@code int} representation of the ID.
	 *
	 * @return the {@code int}
	 */
	public int getValue()
	{
		return value;
	}

	@Override
	public String toString()
	{
		return String.valueOf(value);
	}
}