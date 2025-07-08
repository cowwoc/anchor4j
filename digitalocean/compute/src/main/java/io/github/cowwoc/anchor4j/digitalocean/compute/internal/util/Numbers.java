package io.github.cowwoc.anchor4j.digitalocean.compute.internal.util;

import java.math.BigDecimal;

/**
 * Number helper functions.
 */
public final class Numbers
{
	/**
	 * Returns a defensive copy of a BigDecimal if needed.
	 *
	 * @param value a BigDecimal
	 * @return a safe instance of BigDecimal that contains the same value
	 * @see <a href="https://stackoverflow.com/a/48878445/14731">BigDecimal is not immutable</a>
	 */
	public static BigDecimal copyOf(BigDecimal value)
	{
		if (value == null || value.getClass() == BigDecimal.class)
			return value;
		return new BigDecimal(value.unscaledValue(), value.scale());
	}

	private Numbers()
	{
	}
}