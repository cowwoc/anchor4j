package io.github.cowwoc.anchor4j.digitalocean.database.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.resource.ComputeParser;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.ComputeRegion;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient;
import io.github.cowwoc.anchor4j.digitalocean.database.client.DatabaseClient;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.Database;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.Database.Id;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.DatabaseCreator;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.DatabaseType;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class DefaultDatabaseClient extends AbstractDigitalOceanInternalClient
	implements DatabaseClient
{
	private final DatabaseParser databaseParser = new DatabaseParser();
	private final ComputeParser computeParser = new ComputeParser();

	/**
	 * Creates a new DefaultDatabaseClient.
	 */
	public DefaultDatabaseClient()
	{
	}

	/**
	 * @return a {@code DatabaseParser}
	 */
	public DatabaseParser getDatabaseParser()
	{
		return databaseParser;
	}

	/**
	 * @return a {@code ComputeParser}
	 */
	public ComputeParser getComputeParser()
	{
		return computeParser;
	}

	@Override
	public DatabaseType getDatabaseType(DatabaseType.Id id) throws IOException, InterruptedException
	{
		JsonNode options = getOptions(id);

		Set<ComputeRegion.Id> regions = databaseParser.getElements(options, "regions",
			computeParser::regionIdFromServer);
		Set<String> versions = databaseParser.getElements(options, "versions", JsonNode::textValue);

		JsonNode layoutsNode = options.get("layouts");
		Map<Integer, Set<DropletType.Id>> nodeCountToDropletTypes = new HashMap<>();
		for (JsonNode layout : layoutsNode)
		{
			int nodeCount = databaseParser.getInt(layout, "num_nodes");
			JsonNode dropletTypesNode = layout.get("sizes");
			Set<DropletType.Id> dropletTypes = new HashSet<>();
			for (JsonNode node : dropletTypesNode)
				dropletTypes.add(DropletType.id(node.textValue()));
			nodeCountToDropletTypes.put(nodeCount, dropletTypes);
		}

		Map<String, Instant> versionToEndOfLife = new HashMap<>();
		Map<String, Instant> versionToEndOfAvailability = new HashMap<>();
		JsonNode versionAvailability = options.get("version_availability");
		for (JsonNode node : versionAvailability)
		{
			String version = node.get("version").textValue();
			Instant endOfLife = Instant.parse(node.get("end_of_life").textValue());
			versionToEndOfLife.put(version, endOfLife);

			Instant endOfAvailability = Instant.parse(node.get("end_of_availability").textValue());
			versionToEndOfAvailability.put(version, endOfAvailability);
		}

		return new DatabaseType(id, regions, versions, nodeCountToDropletTypes, versionToEndOfLife,
			versionToEndOfAvailability);
	}

	/**
	 * Returns the options that are available for this database type.
	 *
	 * @param id the database type
	 * @return an empty set if no matches are found
	 * @throws NullPointerException  if {@code id} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	private JsonNode getOptions(DatabaseType.Id id) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Databases/operation/databases_list_options
		URI uri = REST_SERVER.resolve("v2/databases/options");
		return getResource(uri, body ->
		{
			JsonNode optionsNode = body.get("options");
			return optionsNode.get(databaseParser.databaseTypeIdToServer(id));
		});
	}

	@Override
	public Set<Database> getDatabases() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Databases/operation/databases_list_clusters
		return getElements(REST_SERVER.resolve("v2/databases"), Map.of(), body ->
		{
			Set<Database> databases = new HashSet<>();
			for (JsonNode database : body.get("databases"))
				databases.add(databaseParser.databaseFromServer(this, database));
			return databases;
		});
	}

	@Override
	public Database getDatabase(Predicate<Database> predicate) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Databases/operation/databases_list_clusters
		return getElement(REST_SERVER.resolve("v2/databases"), Map.of(), body ->
		{
			for (JsonNode database : body.get("databases"))
			{
				Database candidate = databaseParser.databaseFromServer(this, database);
				if (predicate.test(candidate))
					return candidate;
			}
			return null;
		});
	}

	@Override
	public Database getDatabase(Id id) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Databases/operation/databases_get_cluster
		return getResource(REST_SERVER.resolve("v2/databases/" + id), body ->
		{
			JsonNode database = body.get("database");
			return databaseParser.databaseFromServer(this, database);
		});
	}

	@Override
	public DatabaseCreator createDatabase(DatabaseClient client, String name, DatabaseType databaseType,
		int numberOfStandbyNodes, DropletType.Id dropletType, Region.Id region)
	{
		return new DefaultDatabaseCreator(client, name, databaseType, numberOfStandbyNodes, dropletType, region);
	}
}