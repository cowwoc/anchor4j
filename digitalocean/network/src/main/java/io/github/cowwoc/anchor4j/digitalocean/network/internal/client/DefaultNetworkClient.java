package io.github.cowwoc.anchor4j.digitalocean.network.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient;
import io.github.cowwoc.anchor4j.digitalocean.network.client.NetworkClient;
import io.github.cowwoc.anchor4j.digitalocean.network.internal.resource.DefaultVpc;
import io.github.cowwoc.anchor4j.digitalocean.network.internal.resource.NetworkParser;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public class DefaultNetworkClient extends AbstractDigitalOceanInternalClient
	implements NetworkClient
{
	private final NetworkParser parser = new NetworkParser();

	/**
	 * Creates a new DefaultNetworkClient.
	 */
	public DefaultNetworkClient()
	{
	}

	/**
	 * Returns the parser.
	 *
	 * @return the parser
	 */
	public NetworkParser getParser()
	{
		return parser;
	}

	@Override
	public Set<Vpc> getVpcs() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/VPCs/operation/vpcs_list
		return getElement(REST_SERVER.resolve("v2/vpcs"), Map.of(), body ->
		{
			Set<Vpc> defaultVpcs = new HashSet<>();
			for (JsonNode sshKey : body.get("vpcs"))
				defaultVpcs.add(parser.getVpc(sshKey));
			return defaultVpcs;
		});
	}

	@Override
	public Vpc getVpc(Predicate<Vpc> predicate) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/VPCs/operation/vpcs_list
		return getElement(REST_SERVER.resolve("v2/vpcs"), Map.of(), body ->
		{
			for (JsonNode vpcNode : body.get("vpcs"))
			{
				DefaultVpc candidate = parser.getVpc(vpcNode);
				if (predicate.test(candidate))
					return candidate;
			}
			return null;
		});
	}

	@Override
	public Vpc getVpc(Vpc.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/VPCs/operation/vpcs_get
		return getResource(REST_SERVER.resolve("v2/vpcs/" + id), body ->
		{
			JsonNode vpc = body.get("vpc");
			return parser.getVpc(vpc);
		});
	}
}