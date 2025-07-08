package io.github.cowwoc.anchor4j.digitalocean.core.exception;

import java.io.Serial;

/**
 * Thrown if a referenced resource could not be found.
 */
public class ResourceNotFoundException extends Exception
{
	@Serial
	private static final long serialVersionUID = 0L;

	/**
	 * Creates a new instance.
	 *
	 * @param message an explanation of what went wrong
	 */
	public ResourceNotFoundException(String message)
	{
		super(message);
	}
}