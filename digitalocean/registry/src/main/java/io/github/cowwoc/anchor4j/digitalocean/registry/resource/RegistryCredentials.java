package io.github.cowwoc.anchor4j.digitalocean.registry.resource;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Credentials for a container registry.
 *
 * @param username the username
 * @param password the password
 */
public record RegistryCredentials(String username, String password)
{
	/**
	 * Creates a new instance.
	 *
	 * @param username the username
	 * @param password the password
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if any of the arguments contain leading or trailing whitespace or are
	 *                                  empty
	 */
	public RegistryCredentials
	{
		requireThat(username, "username").isStripped().isNotEmpty();
		requireThat(password, "password").isStripped().isNotEmpty();
	}
}