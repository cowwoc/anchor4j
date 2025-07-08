package io.github.cowwoc.anchor4j.buildx.client;

import io.github.cowwoc.anchor4j.buildx.internal.client.DefaultBuildXClient;
import io.github.cowwoc.anchor4j.container.core.client.ContainerClient;

import java.io.IOException;
import java.time.Duration;

/**
 * A Docker BuildX client.
 */
public interface BuildXClient extends ContainerClient
{
	/**
	 * Returns a client that uses the {@code buildx} executable located in the {@code PATH} environment
	 * variable.
	 *
	 * @return the client
	 * @throws IOException if an I/O error occurs while building the client
	 */
	static BuildXClient build() throws IOException
	{
		return new DefaultBuildXClient();
	}

	@Override
	BuildXClient retryTimeout(Duration duration);
}