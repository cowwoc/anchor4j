package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.docker.resource.ContextEndpoint;

import java.net.URI;
import java.nio.file.Path;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Default implementation of {@code ContextEndpoint.Builder}.
 */
public final class ContextEndpointBuilder implements ContextEndpoint.Builder
{
	private final URI uri;
	private Path caPublicKey;
	private Path clientCertificate;
	private Path clientPrivateKey;

	/**
	 * Creates a builder.
	 *
	 * @param uri the docker engine's URI (e.g. {@code tcp://myserver:2376})
	 * @throws NullPointerException if {@code uri} is null
	 */
	public ContextEndpointBuilder(URI uri)
	{
		requireThat(uri, "uri").isNotNull();
		this.uri = uri;
	}

	@Override
	public ContextEndpointBuilder tls(Path caPublicKey, Path clientCertificate, Path clientPrivateKey)
	{
		requireThat(caPublicKey, "caPublicKey").isNotNull();
		requireThat(clientCertificate, "clientCertificate").isNotNull();
		requireThat(clientPrivateKey, "clientPrivateKey").isNotNull();
		this.caPublicKey = caPublicKey;
		this.clientCertificate = clientCertificate;
		this.clientPrivateKey = clientPrivateKey;
		return this;
	}

	@Override
	public ContextEndpoint build()
	{
		return new ContextEndpoint(uri, caPublicKey, clientCertificate, clientPrivateKey);
	}
}