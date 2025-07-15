package io.github.cowwoc.anchor4j.digitalocean.database.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.cowwoc.anchor4j.core.exception.AccessDeniedException;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.core.util.CreateResult;
import io.github.cowwoc.anchor4j.digitalocean.database.client.DatabaseClient;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.Database;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.DatabaseCreator;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.DatabaseType;
import io.github.cowwoc.anchor4j.digitalocean.network.internal.resource.NetworkParser;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient.REST_SERVER;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static org.eclipse.jetty.http.HttpMethod.POST;
import static org.eclipse.jetty.http.HttpStatus.CREATED_201;
import static org.eclipse.jetty.http.HttpStatus.UNAUTHORIZED_401;
import static org.eclipse.jetty.http.HttpStatus.UNPROCESSABLE_ENTITY_422;

public final class DefaultDatabaseCreator implements DatabaseCreator
{
	private final DefaultDatabaseClient client;
	private final String name;
	private final DatabaseType databaseType;
	private String version;
	private final int numberOfStandbyNodes;
	private final DropletType.Id dropletType;
	private final Region.Id region;
	private Vpc.Id vpc;
	private final Set<String> tags = new HashSet<>();
	private String projectId = "";
	private final Set<FirewallRuleBuilder> firewallRules = new HashSet<>();
	private int additionalStorageInMiB;
	private RestoreFrom restoreFrom;

	/**
	 * Creates a DefaultDatabaseCreator.
	 *
	 * @param client               the client configuration
	 * @param name                 the name of the cluster
	 * @param databaseType         the type of the database
	 * @param numberOfStandbyNodes the number of nodes in the cluster. The cluster includes one primary node,
	 *                             and may include one or two standby nodes.
	 * @param dropletType          the machine type of the droplet
	 * @param region               the region to create the cluster in
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code name} contains leading or trailing whitespace or is
	 *                                    empty.</li>
	 *                                    <li>{@code numberOfStandbyNodes} is negative, or greater than 2.</li>
	 *                                  </ul>
	 * @throws IllegalStateException    if the client is closed
	 */
	public DefaultDatabaseCreator(DatabaseClient client, String name, DatabaseType databaseType,
		int numberOfStandbyNodes, DropletType.Id dropletType, Region.Id region)
	{
		requireThat(client, "client").isNotNull();
		requireThat(name, "name").isStripped().isNotEmpty();
		requireThat(databaseType, "databaseType").isNotNull();
		requireThat(numberOfStandbyNodes, "numberOfStandbyNodes").isBetween(0, true, 2, true);
		requireThat(dropletType, "dropletType").isNotNull();
		requireThat(region, "region").isNotNull();
		this.client = (DefaultDatabaseClient) client;
		this.name = name;
		this.databaseType = databaseType;
		this.numberOfStandbyNodes = numberOfStandbyNodes;
		this.dropletType = dropletType;
		this.region = region;
	}

	@Override
	public DatabaseCreator version(String version)
	{
		requireThat(version, "version").isStripped();
		this.version = version;
		return this;
	}

	@Override
	public DatabaseCreator vpc(Vpc.Id vpc)
	{
		this.vpc = vpc;
		return this;
	}

	@Override
	public DatabaseCreator tag(String tag)
	{
		// Discovered empirically: DigitalOcean drops all tags silently if any of them contain invalid characters.
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Databases/operation/tags_create
		requireThat(tag, "tag").matches("^[a-zA-Z0-9_\\-:]+$").length().isLessThanOrEqualTo(255);
		this.tags.add(tag);
		return this;
	}

	@Override
	public DatabaseCreator tags(Collection<String> tags)
	{
		requireThat(tags, "tags").isNotNull().doesNotContain(null);
		this.tags.clear();
		for (String tag : tags)
		{
			requireThat(tag, "tag").withContext(tags, "tags").matches("^[a-zA-Z0-9_\\-:]+$").
				length().isLessThanOrEqualTo(255);
			this.tags.add(tag);
		}
		return this;
	}

	@Override
	public DatabaseCreator projectId(String projectId)
	{
		requireThat(projectId, "projectId").isStripped();
		this.projectId = projectId;
		return this;
	}

	@Override
	public DatabaseCreator firewallRule(FirewallRuleBuilder firewallRule)
	{
		requireThat(firewallRule, "firewallRule").isNotNull();
		this.firewallRules.add(firewallRule);
		return this;
	}

	@Override
	public DatabaseCreator firewallRules(Collection<FirewallRuleBuilder> firewallRules)
	{
		requireThat(firewallRules, "firewallRules").isNotNull().doesNotContain(null);
		this.firewallRules.clear();
		this.firewallRules.addAll(firewallRules);
		return this;
	}

	@Override
	public void additionalStorageInMiB(int additionalStorageInMiB)
	{
		requireThat(additionalStorageInMiB, "additionalStorageInMiB").isNotNegative();
		this.additionalStorageInMiB = additionalStorageInMiB;
	}

	@Override
	public DatabaseCreator restoreFrom(RestoreFrom restoreFrom)
	{
		this.restoreFrom = restoreFrom;
		return this;
	}

	/**
	 * Creates a new database cluster.
	 *
	 * @return the new or conflicting cluster
	 * @throws IllegalArgumentException if the {@code dropletType} is not supported by managed databases
	 * @throws IllegalStateException    if the client is closed
	 * @throws AccessDeniedException    if the client does not have sufficient privileges to execute this
	 *                                  request
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	public CreateResult<Database> apply() throws AccessDeniedException, IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Databases/operation/databases_create_cluster
		JsonMapper jm = client.getJsonMapper();
		DatabaseParser parser = client.getDatabaseParser();
		NetworkParser networkParser = client.getNetworkParser();
		ObjectNode requestBody = jm.createObjectNode().
			put("name", name).
			put("engine", parser.databaseTypeIdToServer(databaseType.id())).
			put("num_nodes", numberOfStandbyNodes + 1).
			put("size", dropletType.toString()).
			put("region", networkParser.regionIdToServer(region));
		if (!version.isEmpty())
			requestBody.put("version", version);
		if (vpc != null)
			requestBody.put("private_network_uuid", vpc.getValue());
		if (!tags.isEmpty())
		{
			ArrayNode tagsNode = requestBody.putArray("tags");
			for (String tag : tags)
				tagsNode.add(tag);
		}
		if (!projectId.isEmpty())
			requestBody.put("project_id", projectId);
		if (!firewallRules.isEmpty())
		{
			ArrayNode firewallRulesNode = requestBody.putArray("rules");
			for (FirewallRuleBuilder firewallRuleBuilder : firewallRules)
				firewallRulesNode.add(parser.firewallRuleToServer(firewallRuleBuilder));
		}
		if (additionalStorageInMiB > 0)
			requestBody.put("storage_size_mib", additionalStorageInMiB);
		if (restoreFrom != null)
			requestBody.set("backup_restore", parser.restoreFromToServer(restoreFrom));

		Request request = client.createRequest(REST_SERVER.resolve("v2/databases"), requestBody).
			method(POST);
		Response serverResponse = client.send(request);
		return switch (serverResponse.getStatus())
		{
			case CREATED_201 ->
			{
				ContentResponse contentResponse = (ContentResponse) serverResponse;
				JsonNode body = client.getResponseBody(contentResponse);
				yield CreateResult.created(parser.databaseFromServer(body.get("database")));
			}
			case UNAUTHORIZED_401 ->
			{
				ContentResponse contentResponse = (ContentResponse) serverResponse;
				JsonNode json = client.getResponseBody(contentResponse);
				throw new AccessDeniedException(json.get("message").textValue());
			}
			case UNPROCESSABLE_ENTITY_422 ->
			{
				// Discovered empirically: not all droplet types are acceptable.
				ContentResponse contentResponse = (ContentResponse) serverResponse;
				JsonNode json = client.getResponseBody(contentResponse);
				String message = json.get("message").textValue();
				switch (message)
				{
					case "invalid size" -> throw new IllegalArgumentException("dropletType may not be " + dropletType);
					case "cluster name is not available" ->
					{
						Database conflict = client.getDatabaseCluster(cluster -> cluster.getName().equals(name));
						if (conflict != null)
							yield CreateResult.conflictedWith(conflict);
						throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
							"Request: " + client.toString(request));
					}
					default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) +
						"\n" +
						"Request: " + client.toString(request));
				}
			}
			default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
				"Request: " + client.toString(request));
		};
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultDatabaseCreator.class).
			add("name", name).
			add("databaseType", databaseType).
			add("version", version).
			add("numberOfStandbyNodes", numberOfStandbyNodes).
			add("dropletType", dropletType).
			add("region", region).
			add("vpc", vpc).
			add("tags", tags).
			add("projectId", projectId).
			add("firewallRules", firewallRules).
			add("additionalStorageInMiB", additionalStorageInMiB).
			add("restoreFrom", restoreFrom).
			toString();
	}
}