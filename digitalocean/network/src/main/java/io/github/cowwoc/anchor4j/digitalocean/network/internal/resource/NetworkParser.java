package io.github.cowwoc.anchor4j.digitalocean.network.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.core.client.Client;
import io.github.cowwoc.anchor4j.core.internal.resource.AbstractParser;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region.Id;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;

/**
 * Parses server responses.
 */
public final class NetworkParser extends AbstractParser
{
	/**
	 * Creates a NetworkParser.
	 *
	 * @param client the client configuration
	 */
	public NetworkParser(Client client)
	{
		super(client);
	}

	@Override
	protected Client getClient()
	{
		return client;
	}

	/**
	 * Converts VPC from its server representation.
	 *
	 * @param json the server representation
	 * @return the VPC
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	public Vpc vpcFromServer(JsonNode json)
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/VPCs/operation/vpcs_get
		Vpc.Id id = Vpc.id(json.get("id").textValue());
		Region.Id region = regionIdFromServer(json.get("region"));
		return new DefaultVpc(id, region);
	}

	/**
	 * Convert a Region.Id from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the ID of the region
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	public Id regionIdFromServer(JsonNode json)
	{
		String value = json.textValue();
		return switch (value)
		{
			case "atl1" -> Id.ATLANTA1;
			case "nyc1" -> Id.NEW_YORK1;
			case "nyc2" -> Id.NEW_YORK2;
			case "nyc3" -> Id.NEW_YORK3;
			case "sfo1" -> Id.SAN_FRANCISCO1;
			case "sfo2" -> Id.SAN_FRANCISCO2;
			case "sfo3" -> Id.SAN_FRANCISCO3;
			case "ams2" -> Id.AMSTERDAM2;
			case "ams3" -> Id.AMSTERDAM3;
			case "sgp1" -> Id.SINGAPORE1;
			case "lon1" -> Id.LONDON1;
			case "fra1" -> Id.FRANCE1;
			case "tor1" -> Id.TORONTO1;
			case "blr1" -> Id.BANGALORE1;
			case "syd1" -> Id.SYDNEY1;
			default -> throw new IllegalArgumentException("Unsupported value: " + value);
		};
	}

	/**
	 * Convert a Region.Id to its server representation.
	 *
	 * @param value the ID of the region
	 * @return the server representation
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	public String regionIdToServer(Region.Id value)
	{
		return switch (value)
		{
			case ATLANTA1 -> "atl1";
			case NEW_YORK1 -> "nyc1";
			case NEW_YORK2 -> "nyc2";
			case NEW_YORK3 -> "nyc3";
			case SAN_FRANCISCO1 -> "sfo1";
			case SAN_FRANCISCO2 -> "sfo2";
			case SAN_FRANCISCO3 -> "sfo3";
			case AMSTERDAM2 -> "ams2";
			case AMSTERDAM3 -> "ams3";
			case SINGAPORE1 -> "sgp1";
			case LONDON1 -> "lon1";
			case FRANCE1 -> "fra1";
			case TORONTO1 -> "tor1";
			case BANGALORE1 -> "blr1";
			case SYDNEY1 -> "syd1";
		};
	}
}