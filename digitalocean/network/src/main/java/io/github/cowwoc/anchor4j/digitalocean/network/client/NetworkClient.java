package io.github.cowwoc.anchor4j.digitalocean.network.client;

import io.github.cowwoc.anchor4j.digitalocean.core.client.DigitalOceanClient;
import io.github.cowwoc.anchor4j.digitalocean.network.internal.client.DefaultNetworkClient;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;

import java.io.IOException;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A DigitalOcean network client.
 */
public interface NetworkClient extends DigitalOceanClient
{
	/**
	 * Returns a client.
	 *
	 * @return the client
	 * @throws IOException if an I/O error occurs while building the client
	 */
	static NetworkClient build() throws IOException
	{
		return new DefaultNetworkClient();
	}

	/**
	 * Returns all the VPCs.
	 *
	 * @return an empty set if no match is found
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Set<Vpc> getVpcs() throws IOException, InterruptedException;

	/**
	 * Returns the first VPC that matches a predicate.
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
	Vpc getVpc(Predicate<Vpc> predicate) throws IOException, InterruptedException;

	/**
	 * Looks up a VPC by its ID.
	 *
	 * @param id the ID of the VPC
	 * @return null if no match is found
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} is not a valid UUID per RFC 9562
	 * @throws IllegalStateException    if the client is closed
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	Vpc getVpc(Vpc.Id id) throws IOException, InterruptedException;
}