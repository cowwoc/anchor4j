package io.github.cowwoc.anchor4j.core.internal.client;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

/**
 * Represents an operation that may fail due to intermittent I/O errors.
 *
 * @param <V> the type of value returned by the operation
 */
public interface Operation<V>
{
	/**
	 * Runs the operation.
	 *
	 * @param deadline the absolute time by which the operation must succeed. The method will retry failed
	 *                 operations while the current time is before this value.
	 * @return the value returned by the operation
	 * @throws NullPointerException if {@code until} is null
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 * @throws TimeoutException     if the deadline expires before the operation succeeds, and no other
	 *                              exception was thrown to indicate the failure
	 */
	V run(Instant deadline) throws InterruptedException, IOException, TimeoutException;
}