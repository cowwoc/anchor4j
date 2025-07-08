package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.internal.resource.ContextEndpointBuilder;

import java.net.URI;
import java.nio.file.Path;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * The endpoint of a Docker Engine.
 *
 * @param uri               the docker engine's URI (e.g. {@code tcp://myserver:2376})
 * @param caPublicKey       the path to the Certificate Authority (CA) certificate used to verify the docker
 *                          server's certificate, or {@code null} if not used
 * @param clientCertificate the path to the docker client's X.509 certificate, or {@code null} if not used
 * @param clientPrivateKey  the path to the docker client's private key, or {@code null} if not used
 */
public record ContextEndpoint(URI uri, Path caPublicKey, Path clientCertificate, Path clientPrivateKey)
{
	/**
	 * Creates an endpoint.
	 *
	 * @param uri               the docker engine's URI (e.g. {@code tcp://myserver:2376})
	 * @param caPublicKey       the path to the Certificate Authority (CA) certificate used to verify the docker
	 *                          server's certificate, or {@code null} if not used
	 * @param clientCertificate the path to the docker client's X.509 certificate, or {@code null} if not used
	 * @param clientPrivateKey  the path to the docker client's private key, or {@code null} if not used
	 * @throws NullPointerException     if {@code uri} is null
	 * @throws IllegalArgumentException if some but not all of {@code caPublicKey}, {@code clientCertificate},
	 *                                  {@code clientPrivateKey} are {@code null}
	 */
	public ContextEndpoint
	{
		requireThat(uri, "uri").isNotNull();
		boolean anySet = caPublicKey != null || clientCertificate != null || clientPrivateKey != null;
		boolean allSet = caPublicKey != null && clientCertificate != null && clientPrivateKey != null;
		if (anySet && !allSet)
		{
			throw new IllegalArgumentException("Either all or none of the TLS parameters must be set:\n" +
				"caPublicKey      : " + caPublicKey + "\n" +
				"clientCertificate: " + clientCertificate + "\n" +
				"clientPrivateKey : " + clientPrivateKey);
		}
	}

	/**
	 * Returns a {@code ContextEndpoint} builder.
	 *
	 * @param uri the docker engine's URI (e.g. {@code tcp://myserver:2376})
	 * @return an endpoint builder
	 * @throws NullPointerException if {@code uri} is null
	 */
	public static Builder builder(URI uri)
	{
		return new ContextEndpointBuilder(uri);
	}

	/**
	 * Builds a {@code ContextEndpoint}.
	 */
	public interface Builder
	{
		/**
		 * Sets the TLS parameters of this endpoint.
		 *
		 * @param caPublicKey       the path to the Certificate Authority (CA) certificate used to verify the
		 *                          docker server's certificate
		 * @param clientCertificate the path to the docker client's X.509 certificate
		 * @param clientPrivateKey  the path to the docker client's private key
		 * @return this
		 * @throws NullPointerException if any of the parameters are null
		 */
		Builder tls(Path caPublicKey, Path clientCertificate, Path clientPrivateKey);

		/**
		 * Builds the endpoint.
		 *
		 * @return the {@code ContextEndpoint}
		 */
		ContextEndpoint build();
	}
}