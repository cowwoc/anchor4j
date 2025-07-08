package io.github.cowwoc.anchor4j.digitalocean.registry.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient;
import io.github.cowwoc.anchor4j.digitalocean.registry.client.RegistryClient;
import io.github.cowwoc.anchor4j.digitalocean.registry.internal.resource.RegistryParser;
import io.github.cowwoc.anchor4j.digitalocean.registry.resource.ContainerRegistry;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;

import java.io.IOException;
import java.nio.file.AccessDeniedException;

import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.eclipse.jetty.http.HttpStatus.UNAUTHORIZED_401;

public class DefaultRegistryClient extends AbstractDigitalOceanInternalClient
	implements RegistryClient
{
	private final RegistryParser parser = new RegistryParser(this);

	/**
	 * Creates a new DefaultRegistryClient.
	 */
	public DefaultRegistryClient()
	{
	}

	/**
	 * Returns the parser.
	 *
	 * @return the parser
	 */
	public RegistryParser getParser()
	{
		return parser;
	}

	@Override
	public ContainerRegistry getRegistry() throws IOException, InterruptedException, AccessDeniedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Container-Registry/operation/registry_get
		Request request = createRequest(REST_SERVER.resolve("v2/registry")).
			method(GET);
		Response serverResponse = send(request);
		switch (serverResponse.getStatus())
		{
			case OK_200 ->
			{
				// success
			}
			case UNAUTHORIZED_401 ->
			{
				ContentResponse contentResponse = (ContentResponse) serverResponse;
				JsonNode json = getResponseBody(contentResponse);
				throw new AccessDeniedException(json.get("message").textValue());
			}
			default -> throw new AssertionError("Unexpected response: " + toString(serverResponse) + "\n" +
				"Request: " + toString(request));
		}
		ContentResponse contentResponse = (ContentResponse) serverResponse;
		JsonNode body = getJsonMapper().readTree(contentResponse.getContentAsString());
		JsonNode registryNode = body.get("registry");
		return parser.getRegistry(registryNode);
	}
}