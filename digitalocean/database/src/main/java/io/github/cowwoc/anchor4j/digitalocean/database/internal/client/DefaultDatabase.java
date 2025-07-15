package io.github.cowwoc.anchor4j.digitalocean.database.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.core.internal.util.RetryDelay;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.util.TimeLimit;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.core.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.Database;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.DatabaseCreator;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.DatabaseType;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.Endpoint;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient.REST_SERVER;
import static io.github.cowwoc.anchor4j.digitalocean.core.internal.client.DigitalOceanInternalClient.PROGRESS_FREQUENCY;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.eclipse.jetty.http.HttpMethod.PUT;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

public final class DefaultDatabase implements Database
{
	private final DefaultDatabaseClient client;
	private final Id id;
	private final String name;
	private final DatabaseType.Id databaseTypeId;
	private final String version;
	private final String semanticVersion;
	private final int numberOfStandbyNodes;
	private final DropletType.Id dropletType;
	private final Region.Id region;
	private final Status status;
	private final Vpc.Id vpc;
	private final Set<String> tags;
	private final Set<String> databaseNames;
	private final OpenSearchDashboard openSearchDashboard;
	private final Connection publicConnection;
	private final Connection privateConnection;
	private final Connection standbyPublicConnection;
	private final Connection standbyPrivateConnection;
	private final Set<User> users;
	private final MaintenanceSchedule maintenanceSchedule;
	private final String projectId;
	private final Set<FirewallRule> firewallRules;
	private final Instant versionEndOfLife;
	private final Instant versionEndOfAvailability;
	private final int additionalStorageInMiB;
	private final Set<Endpoint> metricsEndpoints;
	private final Instant createdAt;
	private final Logger log = LoggerFactory.getLogger(DefaultDatabase.class);

	/**
	 * Creates a new cluster.
	 *
	 * @param client                   the client configuration
	 * @param id                       the ID of the cluster
	 * @param name                     the name of the cluster
	 * @param databaseTypeId           the database type
	 * @param version                  the major version number of the database software
	 * @param semanticVersion          the semantic version number of the database software
	 * @param numberOfStandbyNodes     the number of standby nodes in the cluster. The cluster includes one
	 *                                 primary node, and may include one or two standby nodes.
	 * @param dropletType              the machine type of the nodes
	 * @param region                   the region that the cluster is deployed to
	 * @param status                   the state of the cluster
	 * @param vpc                      the VPC that the cluster is deployed in, or {@code null} if it is
	 *                                 deployed into the default VPC of the region
	 * @param tags                     the tags that are associated with the cluster
	 * @param databaseNames            the names of databases in the cluster
	 * @param openSearchDashboard      (optional) connection details for accessing the OpenSearch dashboard, or
	 *                                 {@code null} if this is not an OpenSearch database
	 * @param publicConnection         (optional) the database's public connection endpoint, or {@code null} if
	 *                                 absent
	 * @param privateConnection        (optional) the database's private connection endpoint (accessible only
	 *                                 from within the VPC), or {@code null} if absent
	 * @param standbyPublicConnection  (optional) the standby instance's public connection endpoint, or
	 *                                 {@code null} if absent
	 * @param standbyPrivateConnection (optional) the standby instance's private connection endpoint (accessible
	 *                                 only from within the VPC), or {@code null} if absent
	 * @param users                    the database users
	 * @param maintenanceSchedule      the maintenance schedule for the cluster
	 * @param projectId                the ID of the project that the cluster is assigned to
	 * @param firewallRules            the cluster's firewall rules
	 * @param versionEndOfLife         the time when this version will no longer be supported, or {@code null}
	 *                                 if this version does not have an end-of-life timeline.
	 * @param versionEndOfAvailability the time when this version will no longer be available for creating new
	 *                                 clusters, or {@code null} if this version does not have an
	 *                                 end-of-availability timeline.
	 * @param additionalStorageInMiB   the amount of additional storage (in MiB) that is accessible to the
	 *                                 cluster
	 * @param metricsEndpoints         the endpoints for accessing the cluster metrics
	 * @param createdAt                the time the cluster was created
	 * @throws NullPointerException     if any of the mandatory arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code name} contains leading or trailing whitespace or is
	 *                                    empty.</li>
	 *                                    <li>{@code numberOfStandbyNodes} is negative, or greater than 2.</li>
	 *                                    <li>{@code additionalStorageInMiB} is negative.</li>
	 *                                  </ul>
	 */
	public DefaultDatabase(DefaultDatabaseClient client, Id id, String name, DatabaseType.Id databaseTypeId,
		String version, String semanticVersion, int numberOfStandbyNodes, DropletType.Id dropletType,
		Region.Id region, Status status, Vpc.Id vpc, Set<String> tags, Set<String> databaseNames,
		OpenSearchDashboard openSearchDashboard, Connection publicConnection, Connection privateConnection,
		Connection standbyPublicConnection, Connection standbyPrivateConnection, Set<User> users,
		MaintenanceSchedule maintenanceSchedule, String projectId, Set<FirewallRule> firewallRules,
		Instant versionEndOfLife, Instant versionEndOfAvailability, int additionalStorageInMiB,
		Set<Endpoint> metricsEndpoints, Instant createdAt)
	{
		requireThat(client, "client").isNotNull();
		requireThat(id, "id").isNotNull();
		requireThat(name, "name").isStripped().isNotEmpty();
		requireThat(databaseTypeId, "databaseTypeId").isNotNull();
		requireThat(version, "version").isStripped().isNotEmpty();
		requireThat(semanticVersion, "semanticVersion").isStripped().isNotEmpty();
		requireThat(numberOfStandbyNodes, "numberOfStandbyNodes").isBetween(0, true, 2, true);
		requireThat(dropletType, "dropletType").isNotNull();
		requireThat(region, "region").isNotNull();
		requireThat(status, "status").isNotNull();
		requireThat(vpc, "vpc").isNotNull();
		requireThat(tags, "tags").isNotNull();
		for (String tag : tags)
			requireThat(tag, "tag").withContext(tags, "tags").isStripped().isNotEmpty();
		requireThat(databaseNames, "databaseNames").isNotNull();
		for (String databaseName : databaseNames)
		{
			requireThat(databaseName, "databaseName").withContext(databaseNames, "databaseNames").
				isStripped().isNotEmpty();
		}
		requireThat(users, "users").isNotNull();
		requireThat(maintenanceSchedule, "maintenanceSchedule").isNotNull();
		requireThat(projectId, "projectId").isStripped().isNotEmpty();
		requireThat(firewallRules, "firewallRules").isNotNull();
		requireThat(additionalStorageInMiB, "additionalStorageInMiB").isNotNegative();
		requireThat(metricsEndpoints, "metricsEndpoints").isNotNull();
		requireThat(createdAt, "createdAt").isNotNull();

		this.client = client;
		this.id = id;
		this.name = name;
		this.databaseTypeId = databaseTypeId;
		this.version = version;
		this.semanticVersion = semanticVersion;
		this.numberOfStandbyNodes = numberOfStandbyNodes;
		this.dropletType = dropletType;
		this.region = region;
		this.status = status;
		this.vpc = vpc;
		this.tags = Set.copyOf(tags);
		this.databaseNames = Set.copyOf(databaseNames);
		this.openSearchDashboard = openSearchDashboard;
		this.publicConnection = publicConnection;
		this.privateConnection = privateConnection;
		this.standbyPublicConnection = standbyPublicConnection;
		this.standbyPrivateConnection = standbyPrivateConnection;
		this.users = Set.copyOf(users);
		this.maintenanceSchedule = maintenanceSchedule;
		this.projectId = projectId;
		this.firewallRules = Set.copyOf(firewallRules);
		this.versionEndOfLife = versionEndOfLife;
		this.versionEndOfAvailability = versionEndOfAvailability;
		this.additionalStorageInMiB = additionalStorageInMiB;
		this.metricsEndpoints = Set.copyOf(metricsEndpoints);
		this.createdAt = createdAt;
	}

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public DatabaseType.Id getDatabaseTypeId()
	{
		return databaseTypeId;
	}

	@Override
	public String getVersion()
	{
		return version;
	}

	@Override
	public String getSemanticVersion()
	{
		return semanticVersion;
	}

	@Override
	public int getNumberOfStandbyNodes()
	{
		return numberOfStandbyNodes;
	}

	@Override
	public DropletType.Id getDropletType()
	{
		return dropletType;
	}

	@Override
	public Region.Id getRegion()
	{
		return region;
	}

	@Override
	public Status getStatus()
	{
		return status;
	}

	@Override
	public Vpc.Id getVpc()
	{
		return vpc;
	}

	@Override
	public Set<String> getTags()
	{
		return tags;
	}

	@Override
	public Set<String> getDatabaseNames()
	{
		return databaseNames;
	}

	@Override
	public OpenSearchDashboard getOpenSearchDashboard()
	{
		return openSearchDashboard;
	}

	@Override
	public Connection getPublicConnection()
	{
		return publicConnection;
	}

	@Override
	public Connection getPrivateConnection()
	{
		return privateConnection;
	}

	@Override
	public Connection getStandbyPublicConnection()
	{
		return standbyPublicConnection;
	}

	@Override
	public Connection getStandbyPrivateConnection()
	{
		return standbyPrivateConnection;
	}

	@Override
	public Set<User> getUsers()
	{
		return users;
	}

	@Override
	public MaintenanceSchedule getMaintenanceSchedule()
	{
		return maintenanceSchedule;
	}

	@Override
	public Database setMaintenanceSchedule(DatabaseCreator.MaintenanceSchedule maintenanceSchedule)
		throws IOException, InterruptedException, ResourceNotFoundException
	{
		DatabaseParser parser = client.getDatabaseParser();
		DatabaseCreator.MaintenanceSchedule maintenanceScheduleForCreator = parser.maintenanceScheduleToCreator(
			this.maintenanceSchedule);
		if (maintenanceSchedule.equals(maintenanceScheduleForCreator))
			return this;
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Databases/operation/databases_update_maintenanceWindow
		URI uri = REST_SERVER.resolve("v2/databases/" + id + "/maintenance");
		Request request = client.createRequest(uri,
				parser.maintenanceScheduleToServer(client, maintenanceScheduleForCreator)).
			method(PUT);
		Response serverResponse = client.send(request);
		switch (serverResponse.getStatus())
		{
			case NO_CONTENT_204 ->
			{
				// success
			}
			case NOT_FOUND_404 -> throw new ResourceNotFoundException("Database: " + getId());
			default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
				"Request: " + client.toString(request));
		}
		return reload();
	}

	@Override
	public String getProjectId()
	{
		return projectId;
	}

	@Override
	public Set<FirewallRule> getFirewallRules()
	{
		return firewallRules;
	}

	@Override
	public Instant getVersionEndOfLife()
	{
		return versionEndOfLife;
	}

	@Override
	public Instant getVersionEndOfAvailability()
	{
		return versionEndOfAvailability;
	}

	@Override
	public int getAdditionalStorageInMiB()
	{
		return additionalStorageInMiB;
	}

	@Override
	public Set<Endpoint> getMetricsEndpoints()
	{
		return metricsEndpoints;
	}

	@Override
	public Instant getCreatedAt()
	{
		return createdAt;
	}

	@Override
	@CheckReturnValue
	public Database reload() throws IOException, InterruptedException
	{
		return client.getDatabaseCluster(id);
	}

	@Override
	public Database waitForStatus(Status status, Duration timeout)
		throws ResourceNotFoundException, IOException, InterruptedException, TimeoutException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Databases/operation/databases_get_cluster
		URI uri = REST_SERVER.resolve("v2/kubernetes/databases/" + id);
		RetryDelay retryDelay = new RetryDelay(Duration.ofSeconds(3), Duration.ofSeconds(30), 2);
		TimeLimit timeLimit = new TimeLimit(timeout);
		Instant timeOfLastStatus = Instant.MIN;
		while (true)
		{
			Request request = client.createRequest(uri).
				method(GET);
			Response serverResponse = client.send(request);
			switch (serverResponse.getStatus())
			{
				case OK_200 ->
				{
					// success
				}
				case NOT_FOUND_404 -> throw new ResourceNotFoundException("Database: " + id);
				default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
					"Request: " + client.toString(request));
			}
			ContentResponse contentResponse = (ContentResponse) serverResponse;
			JsonNode body = client.getResponseBody(contentResponse);
			Database newCluster = client.getDatabaseParser().databaseFromServer(body.get("database"));
			if (newCluster.getStatus().equals(status))
			{
				if (timeOfLastStatus != Instant.MIN)
					log.info("The status of {} is {}", name, status);
				return newCluster;
			}
			if (!timeLimit.getTimeLeft().isPositive())
				throw new TimeoutException("Operation failed after " + timeLimit.getTimeQuota());
			Instant now = Instant.now();
			if (Duration.between(timeOfLastStatus, now).compareTo(PROGRESS_FREQUENCY) >= 0)
			{
				log.info("Waiting for the status of {} to change from {} to {}", name, newCluster.getStatus(),
					status);
				timeOfLastStatus = now;
			}
			retryDelay.sleep();
		}
	}

	@Override
	public void waitForDestroy(Duration timeout)
		throws IOException, TimeoutException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Databases/operation/databases_get_cluster
		URI uri = REST_SERVER.resolve("v2/kubernetes/databases/" + id);
		RetryDelay retryDelay = new RetryDelay(Duration.ofSeconds(3), Duration.ofSeconds(30), 2);
		TimeLimit timeLimit = new TimeLimit(timeout);
		Instant timeOfLastStatus = Instant.MIN;
		DatabaseParser parser = client.getDatabaseParser();
		while (true)
		{
			Request request = client.createRequest(uri).
				method(GET);
			Response serverResponse = client.send(request);
			switch (serverResponse.getStatus())
			{
				case OK_200 ->
				{
					// The cluster is still alive
				}
				case NOT_FOUND_404 ->
				{
					if (timeOfLastStatus != Instant.MIN)
						log.info("{} was destroyed", name);
					return;
				}
				default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
					"Request: " + client.toString(request));
			}
			ContentResponse contentResponse = (ContentResponse) serverResponse;
			JsonNode body = client.getResponseBody(contentResponse);
			Database newCluster = parser.databaseFromServer(body.get("database"));
			if (!timeLimit.getTimeLeft().isPositive())
				throw new TimeoutException("Operation failed after " + timeLimit.getTimeQuota());
			Instant now = Instant.now();
			if (Duration.between(timeOfLastStatus, now).compareTo(PROGRESS_FREQUENCY) >= 0)
			{
				log.info("Waiting for {} to get destroyed. Current state: {}", name, newCluster.getStatus());
				timeOfLastStatus = now;
			}
			retryDelay.sleep();
		}
	}

	@Override
	public void destroy() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Databases/operation/databases_destroy_cluster
		client.destroyResource(REST_SERVER.resolve("v2/databases/" + id));
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultDatabase.class).
			add("id", id).
			add("name", name).
			add("databaseType", databaseTypeId).
			add("version", version).
			add("semanticVersion", semanticVersion).
			add("numberOfStandbyNodes", numberOfStandbyNodes).
			add("dropletType", dropletType).
			add("region", region).
			add("state", status).
			add("vpc", vpc).
			add("tags", tags).
			add("databaseNames", databaseNames).
			add("openSearchDashboard", openSearchDashboard).
			add("publicConnection", publicConnection).
			add("privateConnection", privateConnection).
			add("publicConnection", standbyPublicConnection).
			add("privateConnection", standbyPrivateConnection).
			add("users", users).
			add("maintenanceSchedule", maintenanceSchedule).
			add("projectId", projectId).
			add("firewallRules", firewallRules).
			add("versionEndOfLife", versionEndOfLife).
			add("versionEndOfAvailability", versionEndOfAvailability).
			add("additionalStorageInMiB", additionalStorageInMiB).
			add("metricsEndpointsB", metricsEndpoints).
			add("createdAt", createdAt).
			toString();
	}
}