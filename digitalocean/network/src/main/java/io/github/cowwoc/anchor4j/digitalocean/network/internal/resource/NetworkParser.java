package io.github.cowwoc.anchor4j.digitalocean.network.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.core.internal.resource.AbstractParser;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;

import java.util.Locale;

/**
 * Parses server responses.
 */
public final class NetworkParser extends AbstractParser
{
	/**
	 * Parses the JSON representation of a VPC.
	 *
	 * @param json the JSON representation
	 * @return the VPC
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	public DefaultVpc getVpc(JsonNode json)
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/VPCs/operation/vpcs_get
		Vpc.Id id = Vpc.id(json.get("id").textValue());
		Region.Id region = getRegion(json.get("region"));
		return new DefaultVpc(id, region);
	}

	/**
	 * Parses the JSON representation of a Region.
	 *
	 * @param json the JSON representation
	 * @return the region
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	public Region.Id getRegion(JsonNode json)
	{
		String text = json.textValue();
		return Region.Id.valueOf(text.toUpperCase(Locale.ROOT));
	}
}