package io.github.cowwoc.anchor4j.container.core.exception;

import java.io.IOException;
import java.io.Serial;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Thrown if the Docker context cannot be found or resolved.
 * <p>
 * This may occur if:
 * <ul>
 *   <li>The target host (local or remote) is not reachable or not configured.</li>
 *   <li>The connection method (e.g., UNIX socket, SSH, TCP) does not exist or is misconfigured.</li>
 *   <li>The named context is missing or has been deleted.</li>
 * </ul>
 */
public class ContextNotFoundException extends IOException
{
	@Serial
	private static final long serialVersionUID = 0L;

	/**
	 * Creates an exception.
	 *
	 * @param message an explanation of what went wrong
	 */
	public ContextNotFoundException(String message)
	{
		super(message);
	}

	/**
	 * Creates an exception.
	 *
	 * @param cause the underlying exception
	 * @throws NullPointerException if {@code cause} is null
	 */
	public ContextNotFoundException(Throwable cause)
	{
		super(cause);
		requireThat(cause, "cause").isNotNull();
	}
}