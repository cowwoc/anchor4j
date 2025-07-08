package io.github.cowwoc.anchor4j.digitalocean.core.client;

import io.github.cowwoc.anchor4j.core.client.Client;

import java.time.Duration;

public interface DigitalOceanClient extends Client, AutoCloseable
{
	/**
	 * Sets the access token used for authentication.
	 * <p>
	 * To create an access token:
	 * <ol>
	 *   <li>Go to the <a href="https://cloud.digitalocean.com/account/api/tokens">DigitalOcean Control Panel
	 *   </a></li>
	 *   <li>Click {@code Generate New Token}</li>
	 *   <li>Copy the token</li>
	 * </ol>
	 * <p>
	 *
	 * @param accessToken the DigitalOcean access token
	 * @return this
	 * @throws NullPointerException     if {@code accessToken} is null
	 * @throws IllegalArgumentException if {@code accessToken} contains leading or trailing whitespace or is
	 *                                  empty
	 */
	DigitalOceanClient login(String accessToken);

	@Override
	DigitalOceanClient retryTimeout(Duration duration);

	/**
	 * Determines if the client is closed.
	 *
	 * @return {@code true} if the client is closed
	 */
	boolean isClosed();

	/**
	 * Closes the client.
	 */
	@Override
	void close();
}