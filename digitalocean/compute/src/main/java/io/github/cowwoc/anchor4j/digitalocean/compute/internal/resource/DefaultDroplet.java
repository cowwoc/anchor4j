package io.github.cowwoc.anchor4j.digitalocean.compute.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.client.ComputeClient;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.client.DefaultComputeClient;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.Droplet;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletFeature;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletImage;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import static io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient.REST_SERVER;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static org.eclipse.jetty.http.HttpStatus.CREATED_201;

public final class DefaultDroplet implements Droplet
{
	private final DefaultComputeClient client;
	private final Id id;
	private final String name;
	private final DropletType.Id type;
	private final DropletImage image;
	private final Region.Id region;
	private final Vpc.Id vpc;
	private final Set<InetAddress> addresses;
	private final Set<DropletFeature> features;
	private final Set<String> tags;
	private final Instant createdAt;

	/**
	 * Creates a new droplet.
	 *
	 * @param client    the client configuration
	 * @param id        the ID of the droplet
	 * @param name      the name of the droplet
	 * @param type      the machine type
	 * @param image     the image ID of a public or private image or the slug identifier for a public image that
	 *                  will be used to boot this droplet
	 * @param region    the region that the droplet is deployed in
	 * @param vpc       the VPC that the droplet is deployed in
	 * @param addresses the droplet's IP addresses
	 * @param features  the features that are enabled on the droplet
	 * @param tags      the tags that are associated with the droplet
	 * @param createdAt the time the droplet was created
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if any of the arguments contain leading or trailing whitespace or are
	 *                                  empty
	 * @see ComputeClient#getDefaultVpc(Region.Id)
	 */
	public DefaultDroplet(DefaultComputeClient client, Id id, String name, DropletType.Id type,
		DropletImage image, Region.Id region, Vpc.Id vpc, Set<InetAddress> addresses,
		Set<DropletFeature> features, Set<String> tags, Instant createdAt)
	{
		requireThat(client, "client").isNotNull();
		requireThat(name, "name").isStripped().isNotEmpty();
		requireThat(type, "type").isNotNull();
		requireThat(image, "image").isNotNull();
		requireThat(region, "region").isNotNull();
		requireThat(vpc, "vpc").isNotNull();
		requireThat(addresses, "addresses").isNotNull();
		requireThat(features, "features").isNotNull();
		requireThat(tags, "tags").isNotNull();
		for (String tag : tags)
			requireThat(tag, "tag").withContext(tags, "tags").isStripped().isNotEmpty();
		requireThat(createdAt, "createdAt").isNotNull();

		this.client = client;
		this.id = id;
		this.name = name;
		this.type = type;
		this.image = image;
		this.region = region;
		this.vpc = vpc;
		this.addresses = Set.copyOf(addresses);
		this.features = EnumSet.copyOf(features);
		this.tags = Set.copyOf(tags);
		this.createdAt = createdAt;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public DropletType.Id getType()
	{
		return type;
	}

	@Override
	public DropletImage getImage()
	{
		return image;
	}

	@Override
	public Region.Id getRegion()
	{
		return region;
	}

	@Override
	public Vpc.Id getVpc()
	{
		return vpc;
	}

	@Override
	public Set<InetAddress> getAddresses()
	{
		return addresses;
	}

	@Override
	public Set<DropletFeature> getFeatures()
	{
		return features;
	}

	@Override
	public Set<String> getTags()
	{
		return tags;
	}

	@Override
	public Instant getCreatedAt()
	{
		return createdAt;
	}

	@Override
	@CheckReturnValue
	public Droplet reload() throws IOException, InterruptedException
	{
		return client.getDroplet(id);
	}

	@Override
	public Droplet renameTo(String newName) throws IOException, InterruptedException
	{
		requireThat(newName, "newName").isStripped().isNotEmpty();

		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Droplets/operation/dropletActions_post
		JsonMapper jm = client.getJsonMapper();
		ObjectNode requestBody = jm.createObjectNode().
			put("type", "rename").
			put("name", newName);
		Request request = client.createRequest(REST_SERVER.resolve("v2/droplets/" + id + "/actions"),
			requestBody);
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
		JsonNode responseBody = client.getResponseBody(contentResponse);
		String status = responseBody.get("status").textValue();
		ComputeParser parser = client.getParser();
		while (status.equals("in-progress"))
		{
			// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Droplets/operation/dropletActions_get
			int actionId = parser.getInt(responseBody, "id");
			URI uri = REST_SERVER.resolve("v2/droplets/" + id + "/actions/" + actionId);
			request = client.createRequest(uri);
			serverResponse = client.send(request);
			status = responseBody.get("status").textValue();
		}
		return switch (status)
		{
			case "completed" -> reload();
			case "errored" -> throw new IOException("Failed to rename droplet " + id + " to " + newName);
			default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
				"Request: " + client.toString(request));
		};
	}

	@Override
	public void destroy() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Droplets/operation/droplets_destroy
		client.destroyResource(REST_SERVER.resolve("v2/droplets/" + id));
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultDroplet.class).
			add("id", id).
			add("name", name).
			add("type", type).
			add("image", image).
			add("region", region).
			add("vpc", vpc).
			add("addresses", addresses).
			add("features", features).
			add("tags", tags).
			add("createdAt", createdAt).
			toString();
	}
}