package io.github.cowwoc.anchor4j.digitalocean.registry.client;

import io.github.cowwoc.anchor4j.digitalocean.core.exception.AccessDeniedException;
import io.github.cowwoc.anchor4j.digitalocean.registry.internal.client.DefaultRegistryClient;
import io.github.cowwoc.anchor4j.digitalocean.registry.resource.ContainerRegistry;

import java.io.IOException;

/**
 * A DigitalOcean's container registry client.
 */
public interface RegistryClient
{
	/**
	 * Returns a client.
	 *
	 * @return the client
	 * @throws IOException if an I/O error occurs while building the client
	 */
	static RegistryClient build() throws IOException
	{
		return new DefaultRegistryClient();
	}

	/**
	 * Returns the account's docker registry.
	 *
	 * @return null if the account does not have a container registry
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 * @throws AccessDeniedException if the client does not have sufficient privileges to execute this request
	 */
	ContainerRegistry getRegistry() throws IOException, InterruptedException, AccessDeniedException;
}