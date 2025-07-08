package io.github.cowwoc.anchor4j.digitalocean.compute.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.client.DefaultComputeClient;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.SshPublicKey;

import java.io.IOException;
import java.util.Objects;

import static io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient.REST_SERVER;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * An SSH public key that is registered with the account or team where they were created.
 */
public final class DefaultSshPublicKey implements SshPublicKey
{
	private final DefaultComputeClient client;
	private final Id id;
	private final String name;
	private final String fingerprint;

	/**
	 * Creates a new public key.
	 *
	 * @param client      the client configuration
	 * @param id          the ID of the key
	 * @param name        the name of the key
	 * @param fingerprint the fingerprint of the key
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if any of the arguments contain leading or trailing whitespace or are
	 *                                  empty
	 */
	public DefaultSshPublicKey(DefaultComputeClient client, Id id, String name, String fingerprint)
	{
		assert client != null;
		requireThat(name, "name").isStripped().isNotEmpty();
		requireThat(fingerprint, "fingerprint").isStripped().isNotEmpty();
		this.client = client;
		this.id = id;
		this.name = name;
		this.fingerprint = fingerprint;
	}

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getFingerprint()
	{
		return fingerprint;
	}

	@Override
	public void destroy() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/SSH-Keys/operation/sshKeys_delete
		client.destroyResource(REST_SERVER.resolve("v2/account/keys/" + id));
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof SshPublicKey other && other.getId() == id;
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultSshPublicKey.class).
			add("id", id).
			add("name", name).
			add("fingerprint", fingerprint).
			toString();
	}
}