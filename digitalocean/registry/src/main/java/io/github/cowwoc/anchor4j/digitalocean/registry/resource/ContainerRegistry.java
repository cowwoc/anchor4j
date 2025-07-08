package io.github.cowwoc.anchor4j.digitalocean.registry.resource;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A virtualization container registry.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public interface ContainerRegistry
{
	/**
	 * Returns the hostname of the registry. For example, given {@code registry.digitalocean.com/nasa}, this
	 * method will return {@code registry.digitalocean.com}.
	 *
	 * @return the hostname of the registry
	 */
	String getHostname();

	/**
	 * Returns the name of the registry. For example, given {@code registry.digitalocean.com/nasa}, this method
	 * will return {@code nasa}.
	 *
	 * @return the name of the registry
	 */
	String getName();

	/**
	 * Returns the fully qualified name of the repository (e.g., {@code registry.digitalocean.com/nasa}).
	 *
	 * @return the fully qualified name
	 */
	String getFullyQualifiedName();

	/**
	 * Deletes unused image layers from the registry.
	 *
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	void deleteUnusedLayers() throws IOException, InterruptedException;

	/**
	 * Returns credentials for this registry.
	 *
	 * @param writeAccess {@code true} to grant write access. By default, credentials only grant read access.
	 * @param duration    the duration that the returned credentials will be valid for
	 * @return credentials for this registry
	 * @throws NullPointerException     if {@code duration} is null
	 * @throws IllegalArgumentException if {@code duration} is negative or zero
	 * @throws IllegalStateException    if the client is closed
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	RegistryCredentials getCredentials(boolean writeAccess, Duration duration)
		throws IOException, InterruptedException;

	/**
	 * Returns all the repositories in this registry.
	 *
	 * @return an empty set if no match is found
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Set<ContainerRepository> getRepositories() throws IOException, InterruptedException;

	/**
	 * Returns the first repository that matches a predicate.
	 *
	 * @param predicate the predicate
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code predicate} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	ContainerRepository getRepositoryByPredicate(Predicate<ContainerRepository> predicate)
		throws IOException, InterruptedException;
}