package io.github.cowwoc.anchor4j.digitalocean.registry.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.core.exception.AccessDeniedException;
import io.github.cowwoc.anchor4j.core.id.IntegerId;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.registry.internal.client.DefaultRegistryClient;
import io.github.cowwoc.anchor4j.digitalocean.registry.internal.parser.RegistryParser;
import io.github.cowwoc.anchor4j.digitalocean.registry.resource.ContainerImage;
import io.github.cowwoc.anchor4j.digitalocean.registry.resource.ContainerImage.Layer;
import io.github.cowwoc.anchor4j.digitalocean.registry.resource.Registry;
import io.github.cowwoc.anchor4j.digitalocean.registry.resource.Repository;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient.REST_SERVER;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static org.eclipse.jetty.http.HttpMethod.DELETE;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;
import static org.eclipse.jetty.http.HttpStatus.PRECONDITION_FAILED_412;
import static org.eclipse.jetty.http.HttpStatus.TOO_MANY_REQUESTS_429;
import static org.eclipse.jetty.http.HttpStatus.UNAUTHORIZED_401;

public final class DefaultRepository implements Repository
{
	private final DefaultRegistryClient client;
	private final Registry registry;
	private final String name;

	/**
	 * Creates a snapshot of a container repository's state.
	 *
	 * @param client   the client configuration
	 * @param name     the name of the repository
	 * @param registry the enclosing registry
	 * @throws NullPointerException     if {@code name} or {@code registry} are null
	 * @throws IllegalArgumentException if {@code name} contains leading or trailing whitespace or is empty
	 */
	public DefaultRepository(DefaultRegistryClient client, Registry registry, String name)
	{
		assert client != null;
		requireThat(registry, "registry").isNotNull();
		requireThat(name, "name").isStripped().isNotEmpty();
		this.client = client;
		this.registry = registry;
		this.name = name;
	}

	@Override
	public Registry getRegistry()
	{
		return registry;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getFullyQualifiedName()
	{
		return registry.getFullyQualifiedName() + "/" + name;
	}

	@Override
	public List<ContainerImage> getImages() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Container-Registry/operation/registry_list_repositoryManifests
		URI uri = REST_SERVER.resolve("v2/registry/" + registry.getName() + "/repositories/" + name +
			"/digests");
		return client.getElements(uri, Map.of(), body ->
		{
			List<ContainerImage> images = new ArrayList<>();
			RegistryParser parser = client.getParser();
			for (JsonNode manifest : body.get("manifests"))
				images.add(parser.getImage(this, manifest));
			return images;
		});
	}

	@Override
	public ContainerImage getImage(Predicate<ContainerImage> predicate)
		throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Container-Registry/operation/registry_list_repositoryManifests
		URI uri = REST_SERVER.resolve("v2/registry/" + registry.getName() + "/repositories/" + name +
			"/digests");
		return client.getElement(uri, Map.of(), body ->
		{
			RegistryParser parser = client.getParser();
			for (JsonNode manifest : body.get("manifests"))
			{
				ContainerImage candidate = parser.getImage(this, manifest);
				if (predicate.test(candidate))
					return candidate;
			}
			return null;
		});
	}

	@Override
	public void destroyImage(ContainerImage.Id imageId)
		throws IOException, InterruptedException, AccessDeniedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Container-Registry/operation/registry_delete_repositoryManifest
		Registry registry = getRegistry();
		URI uri = REST_SERVER.resolve("v2/registry/" + registry.getName() + "/repositories/" +
			getName() + "/digests/" + imageId.getValue());
		Request request = client.createRequest(uri).
			method(DELETE);
		Response serverResponse = client.send(request);
		switch (serverResponse.getStatus())
		{
			case NO_CONTENT_204, NOT_FOUND_404 ->
			{
				// success
			}
			case UNAUTHORIZED_401 ->
			{
				ContentResponse contentResponse = (ContentResponse) serverResponse;
				JsonNode json = client.getResponseBody(contentResponse);
				throw new AccessDeniedException(json.get("message").textValue());
			}
			case TOO_MANY_REQUESTS_429 -> throw client.getTooManyRequestsException(serverResponse);
			case PRECONDITION_FAILED_412 ->
			{
				// Example: "manifest is referenced by one or more other manifests" or
				// "delete operations are not available while garbage collection is running"
				ContentResponse contentResponse = (ContentResponse) serverResponse;
				JsonNode json = client.getResponseBody(contentResponse);
				throw new IllegalArgumentException(json.get("message").textValue());
			}
			default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
				"Request: " + client.toString(request));
		}
	}

	@Override
	public void deleteDanglingImages() throws IOException, InterruptedException, AccessDeniedException
	{
		List<ContainerImage> imagesToDelete = getImages();
		Deque<Layer> layersToKeep = new ArrayDeque<>();
		for (ContainerImage image : imagesToDelete)
		{
			if (!image.getTags().isEmpty())
				layersToKeep.addAll(image.getLayers());
		}

		// Keep all the layers that tagged images transitively depend on
		Set<Layer> processedLayers = new HashSet<>();
		while (true)
		{
			Layer layer = layersToKeep.poll();
			if (layer == null)
				break;
			if (processedLayers.add(layer))
			{
				for (ContainerImage image : new ArrayList<>(imagesToDelete))
				{
					if (image.getId().equals(layer.id()))
					{
						imagesToDelete.remove(image);
						layersToKeep.addAll(image.getLayers());
					}
				}
			}
		}

		for (ContainerImage image : imagesToDelete)
			image.destroy();
		registry.deleteUnusedLayers();
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(registry, name);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Repository other && other.getRegistry().equals(registry) &&
			other.getName().equals(name);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultRepository.class).
			add("registry", registry).
			add("name", name).
			toString();
	}

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	public static final class Id extends IntegerId
	{
		/**
		 * @param value a server-side identifier
		 */
		private Id(int value)
		{
			super(value);
		}
	}
}