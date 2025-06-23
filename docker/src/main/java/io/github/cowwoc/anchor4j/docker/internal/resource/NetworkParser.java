package io.github.cowwoc.anchor4j.docker.internal.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.cowwoc.anchor4j.core.internal.resource.AbstractParser;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.NetworkState;
import io.github.cowwoc.anchor4j.docker.resource.NetworkState.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses server responses to {@code Network} commands.
 */
public final class NetworkParser extends AbstractParser
{
	private static final Pattern NOT_FOUND = Pattern.compile(
		"Error response from daemon: network .+? not found");

	/**
	 * Creates a parser.
	 *
	 * @param client the client configuration
	 */
	public NetworkParser(InternalDocker client)
	{
		super(client);
	}

	@Override
	protected InternalDocker getClient()
	{
		return (InternalDocker) super.getClient();
	}

	/**
	 * @param result the result of executing a command
	 * @return null if no match is found
	 */
	public NetworkState getState(CommandResult result)
	{
		if (result.exitCode() != 0)
		{
			Matcher matcher = NOT_FOUND.matcher(result.stderr());
			if (matcher.matches())
				return null;
			throw result.unexpectedResponse();
		}
		JsonMapper jm = getClient().getJsonMapper();
		try
		{
			JsonNode json = jm.readTree(result.stdout());
			assert json.size() == 1 : json;
			JsonNode network = json.get(0);

			String name = network.get("Name").textValue();
			String id = network.get("Id").textValue();

			JsonNode ipAddressManagement = network.get("IPAM");
			JsonNode configNode = ipAddressManagement.get("Config");
			List<Configuration> configurations = new ArrayList<>(configNode.size());
			if (!configNode.isNull())
			{
				for (JsonNode entry : configNode)
				{
					String subnet = entry.get("Subnet").textValue();
					String gateway = entry.get("Gateway").textValue();
					configurations.add(new Configuration(subnet, gateway));
				}
			}
			return new NetworkState(getClient(), id, name, configurations);
		}
		catch (JsonProcessingException e)
		{
			throw new AssertionError(e);
		}
	}
}