package io.github.cowwoc.anchor4j.digitalocean.compute.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.cowwoc.anchor4j.digitalocean.compute.client.ComputeClient;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.resource.ComputeParser;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.resource.DefaultDropletCreator;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.resource.DefaultSshPublicKey;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.util.SshKeys;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.ComputeRegion;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.Droplet;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletCreator;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletImage;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.SshPublicKey;
import io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient;
import io.github.cowwoc.anchor4j.digitalocean.network.internal.resource.NetworkParser;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region.Id;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;
import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.eclipse.jetty.http.HttpMethod.POST;
import static org.eclipse.jetty.http.HttpStatus.CREATED_201;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

public class DefaultComputeClient extends AbstractDigitalOceanInternalClient
	implements ComputeClient
{
	private static final String DROPLET_METADATA = "http://169.254.169.254";
	private final ComputeParser computeParser = new ComputeParser();
	private final NetworkParser networkParser = new NetworkParser();

	/**
	 * Creates a new DefaultComputeClient.
	 */
	public DefaultComputeClient()
	{
	}

	/**
	 * Returns the parser.
	 *
	 * @return the parser
	 */
	public ComputeParser getParser()
	{
		return computeParser;
	}

	@Override
	public Vpc getDefaultVpc(Region.Id region) throws IOException, InterruptedException
	{
		requireThat(region, "region").isNotNull();

		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/VPCs/operation/vpcs_list
		return getElement(REST_SERVER.resolve("v2/vpcs"), Map.of(), body ->
		{
			for (JsonNode vpc : body.get("vpcs"))
			{
				boolean isDefault = computeParser.getBoolean(vpc.get("default"), "default");
				if (!isDefault)
					continue;
				Region.Id actualRegion = computeParser.regionIdFromServer(vpc.get("region"));
				if (actualRegion.equals(region))
					return networkParser.getVpc(vpc);
			}
			return null;
		});
	}

	@Override
	public Set<ComputeRegion> getRegions(boolean canCreateDroplets) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Regions/operation/regions_list
		return getElements(REST_SERVER.resolve("v2/regions"), Map.of(), body ->
		{
			Set<ComputeRegion> regions = new HashSet<>();
			for (JsonNode region : body.get("regions"))
			{
				ComputeRegion.Id candidateId = computeParser.regionIdFromServer(region);
				ComputeRegion candidate = getRegion(candidateId);
				if (candidate.canCreateDroplets() || !canCreateDroplets)
					regions.add(candidate);
			}
			return regions;
		});
	}

	@Override
	public ComputeRegion getRegion(Predicate<ComputeRegion> predicate) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Regions/operation/regions_list
		return getElement(REST_SERVER.resolve("v2/regions"), Map.of(), body ->
		{
			for (JsonNode regionNode : body.get("regions"))
			{
				ComputeRegion candidate = computeParser.regionFromServer(regionNode);
				if (predicate.test(candidate))
					return candidate;
			}
			return null;
		});
	}

	@Override
	public ComputeRegion getRegion(Id id) throws IOException, InterruptedException
	{
		return getRegion(region -> region.getId().equals(id));
	}

	@Override
	public Set<DropletType> getDropletTypes() throws IOException, InterruptedException
	{
		return getDropletTypes(true);
	}

	@Override
	public Set<DropletType> getDropletTypes(boolean canCreateDroplets) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/api-reference/#operation/sizes_list
		return getElements(REST_SERVER.resolve("v2/sizes"), Map.of(), body ->
		{
			Set<DropletType> types = new HashSet<>();
			for (JsonNode typeNode : body.get("sizes"))
			{
				DropletType candidate = computeParser.dropletTypeFromServer(typeNode);
				if (candidate.isAvailable() || !canCreateDroplets)
					types.add(candidate);
			}
			return types;
		});
	}

	@Override
	public DropletType getDropletType(Predicate<DropletType> predicate) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/api-reference/#operation/sizes_list
		return getElement(REST_SERVER.resolve("v2/sizes"), Map.of(), body ->
		{
			for (JsonNode typeNode : body.get("sizes"))
			{
				DropletType candidate = computeParser.dropletTypeFromServer(typeNode);
				if (predicate.test(candidate))
					return candidate;
			}
			return null;
		});
	}

	@Override
	public Droplet getDroplet(Droplet.Id id) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/api-reference/#operation/droplets_get
		return getResource(REST_SERVER.resolve("v2/droplets/" + id.getValue()), body ->
		{
			JsonNode droplet = body.get("droplet");
			return computeParser.dropletFromServer(this, droplet);
		});
	}

	@Override
	public Set<Droplet> getDroplets()
		throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/api-reference/#operation/droplets_list
		return getElements(REST_SERVER.resolve("v2/droplets"), Map.of(), body ->
		{
			Set<Droplet> droplets = new HashSet<>();
			for (JsonNode droplet : body.get("droplets"))
				droplets.add(computeParser.dropletFromServer(this, droplet));
			return droplets;
		});
	}

	@Override
	public Droplet getDroplet(Predicate<Droplet> predicate) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/api-reference/#operation/droplets_list
		return getElement(REST_SERVER.resolve("v2/droplets"), Map.of(), body ->
		{
			for (JsonNode droplet : body.get("droplets"))
			{
				Droplet candidate = computeParser.dropletFromServer(this, droplet);
				if (predicate.test(candidate))
					return candidate;
			}
			return null;
		});
	}

	@Override
	public DropletCreator createDroplet(String name, DropletType.Id type, DropletImage image)
	{
		return new DefaultDropletCreator(this, name, type, image);
	}

	@Override
	public Set<SshPublicKey> getSshPublicKeys() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/SSH-Keys/operation/sshKeys_list
		return getElements(REST_SERVER.resolve("v2/account/keys"), Map.of(), body ->
		{
			Set<SshPublicKey> keys = new HashSet<>();
			for (JsonNode sshKey : body.get("ssh_keys"))
				keys.add(computeParser.sshPublicKeyFromServer(this, sshKey));
			return keys;
		});
	}

	@Override
	public SshPublicKey getSshPublicKey(Predicate<SshPublicKey> predicate)
		throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/SSH-Keys/operation/projects_list
		return getElement(REST_SERVER.resolve("v2/account/keys"), Map.of(), body ->
		{
			for (JsonNode sshKey : body.get("ssh_keys"))
			{
				SshPublicKey candidate = computeParser.sshPublicKeyFromServer(this, sshKey);
				if (predicate.test(candidate))
					return candidate;
			}
			return null;
		});
	}

	@Override
	public SshPublicKey getSshPublicKey(SshPublicKey.Id id) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/SSH-Keys/operation/sshKeys_get
		return getResource(REST_SERVER.resolve("v2/account/keys/" + id.getValue()), body ->
		{
			JsonNode key = body.get("ssh_key");
			return computeParser.sshPublicKeyFromServer(this, key);
		});
	}

	@Override
	public SshPublicKey getSshPublicKeyByFingerprint(String fingerprint)
		throws IOException, InterruptedException
	{
		requireThat(fingerprint, "fingerprint").isStripped().isNotEmpty();

		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/SSH-Keys/operation/sshKeys_get
		return getResource(REST_SERVER.resolve("v2/account/keys/" + fingerprint),
			body ->
			{
				JsonNode key = body.get("ssh_key");
				return computeParser.sshPublicKeyFromServer(this, key);
			});
	}

	@Override
	public SshPublicKey createSshPublicKey(String name, PublicKey value)
		throws GeneralSecurityException, IOException, InterruptedException
	{
		requireThat(name, "name").isStripped().isNotEmpty();
		requireThat(value, "value").isNotNull();

		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/SSH-Keys/operation/sshKeys_create
		JsonMapper jm = getJsonMapper();
		SshKeys sshKeys = new SshKeys();
		String openSshRepresentation;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			sshKeys.writePublicKeyAsOpenSsh(value, name, out);
			openSshRepresentation = out.toString();
		}
		catch (IOException | GeneralSecurityException e)
		{
			// Exception never thrown by StringWriter
			throw new AssertionError(e);
		}

		ObjectNode requestBody = jm.createObjectNode().
			put("name", name).
			put("public_key", openSshRepresentation);

		Request request = createRequest(REST_SERVER.resolve("v2/account/keys"), requestBody).
			method(POST);
		Response serverResponse = send(request);
		switch (serverResponse.getStatus())
		{
			case CREATED_201 ->
			{
				// success
			}
			default -> throw new AssertionError("Unexpected response: " + toString(serverResponse) + "\n" +
				"Request: " + toString(request));
		}
		ContentResponse contentResponse = (ContentResponse) serverResponse;
		JsonNode body = getResponseBody(contentResponse);
		JsonNode responseId = body.get("id");
		if (responseId != null && responseId.textValue().equals("unprocessable_entity"))
		{
			String message = body.get("message").textValue();
			if (message.equals("SSH Key is already in use on your account"))
				throw new IllegalArgumentException("An SSH key with the same fingerprint is already registered");
			throw new AssertionError(message);
		}
		JsonNode sshKeyNode = body.get("ssh_key");
		SshPublicKey.Id id = SshPublicKey.id(computeParser.getInt(sshKeyNode, "id"));

		String actualName = sshKeyNode.get("name").textValue();
		assert that(actualName, "actualName").isEqualTo(name, "name").elseThrow();

		MessageDigest md5 = MessageDigest.getInstance("MD5");
		String fingerprint = getFingerprint(value, md5);
		return new DefaultSshPublicKey(this, id, name, fingerprint);
	}

	/**
	 * Returns the fingerprint of a key.
	 *
	 * @param value  a key
	 * @param digest the digest to use for generating the fingerprint
	 * @return the fingerprint of the key
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws GeneralSecurityException if the key is unsupported or invalid
	 */
	private static String getFingerprint(PublicKey value, MessageDigest digest) throws GeneralSecurityException
	{
		SshKeys sshKeys = new SshKeys();
		String fingerprint;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			sshKeys.writeFingerprint(value, digest, out);
			fingerprint = out.toString();
		}
		catch (IOException e)
		{
			// Exception never thrown by ByteArrayOutputStream
			throw new AssertionError(e);
		}
		// DigitalOcean does not include the bit-length and hash type in their fingerprints.
		// Given "256 MD5:[rest of fingerprint]", we only want to return "[rest of fingerprint]".
		int colon = fingerprint.indexOf(':');
		assert that(colon, "colon").isNotNegative().elseThrow();
		return fingerprint.substring(colon + 1);
	}

	@Override
	public Integer getDropletId() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/metadata-api/#operation/getDropletId
		String value = getMetadataValue(URI.create(DROPLET_METADATA + "/metadata/v1/id"));
		if (value == null)
			return null;
		return Integer.parseInt(value);
	}

	@Override
	public String getHostname() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/metadata-api/#operation/getHostname
		return getMetadataValue(URI.create(DROPLET_METADATA + "/metadata/v1/hostname"));
	}

	@Override
	public String getRegion() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/metadata-api/#operation/getRegion
		return getMetadataValue(URI.create(DROPLET_METADATA + "/metadata/v1/region"));
	}

	/**
	 * Returns a metadata value.
	 *
	 * @param uri the URI of the REST endpoint
	 * @return null when running outside a droplet
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code uri} contains leading or trailing whitespace or is empty
	 * @throws IllegalStateException    if the client is closed
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	private String getMetadataValue(URI uri) throws IOException, InterruptedException
	{
		// Reduce the timeout since the server is expected to respond quickly. Additionally, we want to timeout
		// swiftly if the service is unavailable when running outside a droplet.
		Request request = createRequest(uri).
			timeout(1, TimeUnit.SECONDS).
			method(GET);
		Response serverResponse = send(request);
		if (serverResponse.getStatus() != OK_200)
			return null;
		ContentResponse contentResponse = (ContentResponse) serverResponse;
		return contentResponse.getContentAsString();
	}
}