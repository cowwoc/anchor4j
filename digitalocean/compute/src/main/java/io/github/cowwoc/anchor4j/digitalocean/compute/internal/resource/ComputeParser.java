package io.github.cowwoc.anchor4j.digitalocean.compute.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.core.client.Client;
import io.github.cowwoc.anchor4j.core.internal.resource.AbstractParser;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.client.DefaultComputeClient;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.ComputeRegion;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.Droplet;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletFeature;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletImage;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType.DiskConfiguration;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType.GpuConfiguration;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.SshPublicKey;
import io.github.cowwoc.anchor4j.digitalocean.network.internal.resource.NetworkParser;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region.Id;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletFeature.IPV6;
import static io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletFeature.MONITORING;
import static io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient.TIB_TO_GIB;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Parses server responses.
 */
public final class ComputeParser extends AbstractParser
{
	private final NetworkParser networkParser;

	/**
	 * Creates a ComputeParser.
	 *
	 * @param client the client configuration
	 */
	public ComputeParser(Client client)
	{
		super(client);
		this.networkParser = new NetworkParser(client);
	}

	@Override
	protected DefaultComputeClient getClient()
	{
		return (DefaultComputeClient) client;
	}

	/**
	 * Convert a DropletFeature from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the droplet feature
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if no match is found
	 */
	public DropletFeature dropletFeatureFromServer(JsonNode json)
	{
		String name = json.textValue();
		requireThat(name, "name").isStripped().isNotEmpty();
		return DropletFeature.valueOf(name.toUpperCase(Locale.ROOT));
	}

	/**
	 * Convert a DropletFeature to its server representation.
	 *
	 * @param feature the JSON representation
	 * @return the server representation
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if no match is found
	 */
	public String dropletFeatureToServer(DropletFeature feature)
	{
		return feature.name().toLowerCase(Locale.ROOT);
	}

	/**
	 * Convert a DiskConfiguration from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the disk configuration
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	public DiskConfiguration diskConfigurationFromServer(JsonNode json)
	{
		String type = json.get("type").textValue();
		boolean persistent = switch (type)
		{
			case "local" -> true;
			case "scratch" -> false;
			default -> throw new IllegalStateException("Unexpected value: " + type);
		};
		JsonNode sizeNode = json.get("size");
		int amount = getInt(sizeNode, "amount");
		String unit = sizeNode.get("unit").textValue();
		int sizeInMiB = switch (unit)
		{
			case "gib" -> amount * 1024;
			case "tib" -> amount * 1024 * 1024;
			default -> throw new IllegalArgumentException("Unsupported unit: " + unit);
		};
		return new DiskConfiguration(persistent, sizeInMiB);
	}

	/**
	 * Convert a GpuConfiguration from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the GPU configuration
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	public GpuConfiguration gpuConfigurationFromServer(JsonNode json)
	{
		int count = getInt(json, "count");
		String model = json.get("model").textValue();
		JsonNode ramNode = json.get("vram");
		int amount = getInt(ramNode, "amount");
		String unit = json.get("unit").textValue();
		int ramInMiB = switch (unit)
		{
			case "gib" -> amount * 1024;
			case "tib" -> amount * 1024 * 1024;
			default -> throw new IllegalArgumentException("Unsupported unit: " + unit);
		};
		return new GpuConfiguration(count, model, ramInMiB);
	}

	/**
	 * Convert a DropletType from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the droplet type
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	public DropletType dropletTypeFromServer(JsonNode json)
	{
		try
		{
			DropletType.Id id = DropletType.id(json.get("slug").textValue());
			int ramInMB = getInt(json, "memory");
			int vCpus = getInt(json, "vcpus");
			int diskInMb = getInt(json, "disk") * 1024;
			BigDecimal transferInGiB = json.get("transfer").decimalValue().multiply(TIB_TO_GIB);
			BigDecimal costPerHour = json.get("price_hourly").decimalValue();
			BigDecimal costPerMonth = json.get("price_monthly").decimalValue();
			Set<Id> regions = getElements(json, "regions", networkParser::regionIdFromServer);
			boolean available = getBoolean(json, "available");
			String description = json.get("description").textValue();
			Set<DiskConfiguration> diskConfiguration = getElements(json, "disk_info",
				this::diskConfigurationFromServer);
			JsonNode gpuInfoNode = json.get("gpu_info");
			GpuConfiguration gpuConfiguration;
			if (gpuInfoNode == null)
				gpuConfiguration = null;
			else
				gpuConfiguration = gpuConfigurationFromServer(gpuInfoNode);
			return new DefaultDropletType(id, ramInMB, vCpus, diskInMb, transferInGiB, costPerMonth,
				costPerHour, regions, available, description, diskConfiguration, gpuConfiguration);
		}
		catch (IOException | InterruptedException e)
		{
			// Exceptions never thrown by getRegionId() or getDiskConfiguration()
			throw new AssertionError(e);
		}
	}

	/**
	 * Convert a Droplet from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the droplet
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	public Droplet dropletFromServer(JsonNode json)
	{
		// https://docs.digitalocean.com/reference/api/api-reference/#operation/droplets_get
		Droplet.Id id = Droplet.id(getInt(json, "id"));
		String name = json.get("name").textValue();
		Instant createdAt = Instant.parse(json.get("created_at").textValue());
		DropletType.Id type = DropletType.id(json.get("size_slug").textValue());

		DropletImage image = dropletImageFromServer(json.get("image"));
		JsonNode regionNode = json.get("region");
		Region.Id region = networkParser.regionIdFromServer(regionNode.get("slug"));
		JsonNode featuresNode = json.get("features");
		Set<String> featureSet = new HashSet<>();
		featuresNode.forEach(element -> featureSet.add(element.textValue()));
		Set<DropletFeature> features = EnumSet.noneOf(DropletFeature.class);
		if (featureSet.contains("droplet_agent"))
			features.add(MONITORING);
		if (featureSet.contains("ipv6"))
			features.add(IPV6);
		if (featureSet.contains("monitoring"))
			features.add(MONITORING);
		if (featureSet.contains("private_networking"))
			features.add(DropletFeature.PRIVATE_NETWORKING);

		Vpc.Id vpc = Vpc.id(json.get("vpc_uuid").textValue());
		Set<InetAddress> addresses = new HashSet<>();
		JsonNode networks = json.get("networks");
		JsonNode v4 = networks.get("v4");
		for (JsonNode address : v4)
		{
			String value = address.get("ip_address").textValue();
			addresses.add(InetAddress.ofLiteral(value));
		}
		JsonNode v6 = networks.get("v6");
		for (JsonNode address : v6)
		{
			String value = address.get("ip_address").textValue();
			addresses.add(InetAddress.ofLiteral(value));
		}
		try
		{
			Set<String> tags = getElements(json, "tags", JsonNode::textValue);
			return new DefaultDroplet(getClient(), id, name, type, image, region, vpc, addresses, features, tags,
				createdAt);
		}
		catch (IOException | InterruptedException e)
		{
			// Exceptions never thrown by JsonNode::textValue
			throw new AssertionError(e);
		}
	}

	/**
	 * Convert a DropletImage from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the droplet image
	 * @throws NullPointerException if {@code json} is null
	 */
	public DropletImage dropletImageFromServer(JsonNode json)
	{
		try
		{
			DropletImage.Id id = DropletImage.id(getInt(json, "id"));
			String name = json.get("name").textValue();
			String distribution = json.get("distribution").textValue();
			String slug = json.get("slug").textValue();
			boolean isPublic = getBoolean(json, "public");
			Set<Region.Id> regions = getElements(json, "regions", networkParser::regionIdFromServer);
			DropletImage.Type type = dropletImageTypeFromServer(json.get("type"));
			int minDiskSizeInGiB = json.get("min_disk_size").intValue();
			float sizeInGiB = json.get("size_gigabytes").floatValue();
			String description = json.get("description").textValue();
			Set<String> tags = getElements(json, "tags", JsonNode::textValue);
			DropletImage.Status status = dropletImageStatusFromServer(json.get("status"));
			String errorMessage = json.get("error_message").textValue();
			Instant createdAt = Instant.parse(json.get("created_at").textValue());
			return new DefaultDropletImage(id, slug, name, distribution, isPublic, regions, type, minDiskSizeInGiB,
				sizeInGiB, description, tags, status, errorMessage, createdAt);
		}
		catch (IOException | InterruptedException e)
		{
			// Exceptions never thrown by getRegionId() or JsonNode::textValue
			throw new AssertionError(e);
		}
	}

	/**
	 * Convert a DropletImage.Type from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the droplet image type
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if no match is found
	 */
	public DropletImage.Type dropletImageTypeFromServer(JsonNode json)
	{
		return DefaultDropletImage.Type.valueOf(json.textValue().toUpperCase(Locale.ROOT));
	}

	/**
	 * Convert a DropletImage.Status from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the droplet image status
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if no match is found
	 */
	public DropletImage.Status dropletImageStatusFromServer(JsonNode json)
	{
		return DefaultDropletImage.Status.valueOf(json.textValue().toUpperCase(Locale.ROOT));
	}

	/**
	 * Convert a SshPublicKey from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the SSH public key
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	public SshPublicKey sshPublicKeyFromServer(JsonNode json)
	{
		SshPublicKey.Id id = SshPublicKey.id(getInt(json, "id"));
		String name = json.get("name").textValue();
		String fingerprint = json.get("fingerprint").textValue();
		return new DefaultSshPublicKey(getClient(), id, name, fingerprint);
	}

	/**
	 * Convert a ComputeRegion from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the region
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	public ComputeRegion regionFromServer(JsonNode json)
	{
		String name = json.get("name").textValue();
		Region.Id id = networkParser.regionIdFromServer(json.get("slug"));
		try
		{
			Set<ComputeRegion.Feature> features = getElements(json, "features", this::regionFeatureFromServer);

			boolean canCreateDroplets = getBoolean(json, "available");
			Set<DropletType.Id> dropletTypes = getElements(json, "sizes", node ->
				DropletType.id(node.textValue()));
			return new DefaultComputeRegion(id, name, features, canCreateDroplets, dropletTypes);
		}
		catch (IOException | InterruptedException e)
		{
			// Exceptions never thrown by getRegionFeature() or DropletType.id()
			throw new AssertionError(e);
		}
	}

	/**
	 * Convert a ComputeRegion.Feature from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the region feature
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if no match is found
	 */
	public ComputeRegion.Feature regionFeatureFromServer(JsonNode json)
	{
		String name = json.textValue();
		requireThat(name, "name").isStripped().isNotEmpty();
		return ComputeRegion.Feature.valueOf(name.toUpperCase(Locale.ROOT));
	}
}