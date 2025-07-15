package io.github.cowwoc.anchor4j.digitalocean.registry.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.core.internal.util.RetryDelay;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.registry.internal.client.DefaultRegistryClient;
import io.github.cowwoc.anchor4j.digitalocean.registry.internal.parser.RegistryParser;
import io.github.cowwoc.anchor4j.digitalocean.registry.resource.Registry;
import io.github.cowwoc.anchor4j.digitalocean.registry.resource.RegistryCredentials;
import io.github.cowwoc.anchor4j.digitalocean.registry.resource.Repository;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient.REST_SERVER;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.eclipse.jetty.http.HttpMethod.POST;
import static org.eclipse.jetty.http.HttpStatus.CREATED_201;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

public final class DefaultRegistry implements Registry
{
	private final DefaultRegistryClient client;
	private final String name;

	/**
	 * Creates a container registry.
	 *
	 * @param client the client configuration
	 * @param name   the name of the registry
	 * @throws NullPointerException     if {@code name} is null
	 * @throws IllegalArgumentException if {@code name} contains leading or trailing whitespace or is empty
	 */
	public DefaultRegistry(DefaultRegistryClient client, String name)
	{
		assert client != null;
		requireThat(name, "name").isStripped().isNotEmpty();
		this.client = client;
		this.name = name;
	}

	@Override
	public String getHostname()
	{
		return "registry.digitalocean.com";
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getFullyQualifiedName()
	{
		return getHostname() + "/" + getName();
	}

	/**
	 * Deletes unused image layers from the registry.
	 *
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	public void deleteUnusedLayers() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Container-Registry/operation/registry_run_garbageCollection
		URI uri = REST_SERVER.resolve("v2/registry/" + name + "/garbage-collection");
		Request request = client.createRequest(uri).
			method(POST);
		Response serverResponse = client.send(request);
		switch (serverResponse.getStatus())
		{
			case CREATED_201 ->
			{
				// success
			}
			default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
				"Request: " + client.toString(request));
		}
		ContentResponse contentResponse = (ContentResponse) serverResponse;
		JsonNode body = client.getResponseBody(contentResponse);
		JsonNode garbageCollection = body.get("garbage_collection");
		String expectedUuid = garbageCollection.get("uuid").textValue();

		uri = REST_SERVER.resolve("v2/registry/" + name + "/garbage-collection");
		RetryDelay retryDelay = new RetryDelay(Duration.ofSeconds(3), Duration.ofSeconds(30), 2);
		String actualUuid;
		do
		{
			// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Container-Registry/operation/registry_get_garbageCollection
			retryDelay.sleep();
			request = client.createRequest(uri).
				method(GET);
			serverResponse = client.send(request);
			actualUuid = switch (serverResponse.getStatus())
			{
				case OK_200 ->
				{
					contentResponse = (ContentResponse) serverResponse;
					body = client.getResponseBody(contentResponse);
					garbageCollection = body.get("garbage_collection");
					yield garbageCollection.get("uuid").textValue();
				}
				case NOT_FOUND_404 ->
				{
					// Operation complete
					yield "";
				}
				default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) +
					"\n" +
					"Request: " + client.toString(request));
			};
		}
		while (actualUuid.equals(expectedUuid));
	}

	/**
	 * Returns docker credentials for this registry.
	 *
	 * @param writeAccess {@code true} to grant write access. By default, credentials only grant read access.
	 * @param duration    the duration that the returned credentials will be valid for
	 * @return docker credentials for this registry
	 * @throws NullPointerException     if {@code duration} is null
	 * @throws IllegalArgumentException if {@code duration} is negative or zero
	 * @throws IllegalStateException    if the client is closed
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	public RegistryCredentials getCredentials(boolean writeAccess, Duration duration)
		throws IOException, InterruptedException
	{
		requireThat(duration, "duration").isGreaterThan(Duration.ZERO);

		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Container-Registry/operation/registry_get_dockerCredentials
		Request request = client.createRequest(REST_SERVER.resolve("v2/registry/docker-credentials")).
			param("expiry_seconds", String.valueOf(duration.toSeconds())).
			param("read_write", String.valueOf(writeAccess)).
			method(GET);
		Response serverResponse = client.send(request);
		if (serverResponse.getStatus() != OK_200)
		{
			throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
				"Request: " + client.toString(request));
		}
		ContentResponse contentResponse = (ContentResponse) serverResponse;
		JsonNode body = client.getResponseBody(contentResponse);
		JsonNode authsNode = body.get("auths");
		JsonNode registryNode = authsNode.get(getHostname());
		String auth = registryNode.get("auth").textValue();
		String decoded = new String(Base64.getDecoder().decode(auth));
		String[] credentialTokens = decoded.split(":");
		return new RegistryCredentials(credentialTokens[0], credentialTokens[1]);
	}

	@Override
	public List<Repository> getRepositories() throws IOException, InterruptedException
	{
		return client.getRepositories(this);
	}

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
	public Repository getRepository(Predicate<Repository> predicate) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Container-Registry/operation/registry_list_repositoriesV2
		URI uri = REST_SERVER.resolve("v2/registry/" + name + "/repositoriesV2");
		return client.getElement(uri, Map.of(), body ->
		{
			RegistryParser parser = client.getParser();
			for (JsonNode repository : body.get("repositories"))
			{
				Repository candidate = parser.getRepository(this, repository);
				if (predicate.test(candidate))
					return candidate;
			}
			return null;
		});
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Registry other && other.getName().equals(name);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultRegistry.class).
			add("hostname", getHostname()).
			add("name", getName()).
			toString();
	}
}