package io.github.cowwoc.anchor4j.core.migration;

import io.github.cowwoc.anchor4j.core.internal.migration.DefaultDriftDetection;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * A DigitalOcean drift-detection client.
 * <p>
 * Drift detection identifies discrepancies between the expected resource state and the actual resources
 * currently provisioned in the cloud environment.
 */
public interface DriftDetection extends AutoCloseable
{
	/**
	 * Returns a client.
	 *
	 * @return the client
	 * @throws IOException if an I/O error occurs while building the client
	 */
	static DriftDetection build() throws IOException
	{
		return new DefaultDriftDetection();
	}

	/**
	 * Determines if a resource type should be included in drift detection. By default, all resource types are
	 * included.
	 *
	 * @param resourceType a function that returns {@code true} for matching resource types
	 * @return the resources
	 * @throws IllegalStateException if the client is closed
	 */
	DriftDetection includeResourceType(Predicate<? super Class<?>> resourceType);

	/**
	 * Determines if a resource should be included in drift detection. By default, all resources are included.
	 *
	 * @param resource a function that returns {@code true} for matching resources
	 * @return the resources
	 * @throws IllegalStateException if the client is closed
	 */
	DriftDetection includeResource(Predicate<Object> resource);

	/**
	 * Calculates and reports the cloud drift.
	 *
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	void report() throws IOException, InterruptedException;

	/**
	 * Determines if the client is closed.
	 *
	 * @return {@code true} if the client is closed
	 */
	boolean isClosed();

	@Override
	void close();
}