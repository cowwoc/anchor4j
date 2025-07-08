package io.github.cowwoc.anchor4j.digitalocean.database.resource;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * The hostname and port of a public endpoint.
 *
 * @param hostname the hostname
 * @param port     the port
 */
public record Endpoint(String hostname, int port)
{
	/**
	 * Creates a new endpoint.
	 *
	 * @param hostname the hostname
	 * @param port     the port
	 * @throws NullPointerException     if {@code hostname} is null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code hostname} contains whitespace or is empty.</li>
	 *                                    <li>{@code port} is negative or zero.</li>
	 *                                  </ul>
	 */
	public Endpoint
	{
		requireThat(hostname, "hostname").doesNotContainWhitespace().isNotEmpty();
		requireThat(port, "port").isPositive();
	}
}