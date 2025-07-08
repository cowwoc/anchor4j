package io.github.cowwoc.anchor4j.digitalocean.kubernetes.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.cowwoc.anchor4j.core.internal.util.RetryDelay;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.client.ComputeClient;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.util.TimeLimit;
import io.github.cowwoc.anchor4j.digitalocean.core.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesCreator;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesParser;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesVersion;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient.REST_SERVER;
import static io.github.cowwoc.anchor4j.digitalocean.core.internal.client.DigitalOceanInternalClient.PROGRESS_FREQUENCY;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static org.eclipse.jetty.http.HttpMethod.DELETE;
import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.eclipse.jetty.http.HttpMethod.PUT;
import static org.eclipse.jetty.http.HttpStatus.ACCEPTED_202;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

public final class DefaultKubernetes implements Kubernetes
{
	private final DefaultKubernetesClient client;
	private final Id id;
	private final String name;
	private final Region.Id region;
	private final KubernetesVersion version;
	private final String clusterSubnet;
	private final String serviceSubnet;
	private final Vpc.Id vpc;
	private final String ipv4;
	private final String endpoint;
	private final Set<String> tags;
	private final Set<NodePool> nodePools;
	private final MaintenanceSchedule maintenanceSchedule;
	private final boolean autoUpgrade;
	private final Status status;
	private final boolean surgeUpgrade;
	private final boolean highAvailability;
	private final boolean canAccessRegistry;
	private final Instant createdAt;
	private final Instant updatedAt;
	private final Logger log = LoggerFactory.getLogger(DefaultKubernetes.class);

	/**
	 * Creates a new Kubernetes cluster.
	 *
	 * @param client              the client configuration
	 * @param id                  the ID of the cluster
	 * @param name                the name of the cluster
	 * @param region              the region that the droplet is deployed in
	 * @param version             the software version of Kubernetes
	 * @param clusterSubnet       the range of IP addresses for the overlay network of the Kubernetes cluster,
	 *                            in CIDR notation
	 * @param serviceSubnet       the range of IP addresses for the services running in the Kubernetes cluster,
	 *                            in CIDR notation
	 * @param vpc                 the VPC that the cluster is deployed in
	 * @param ipv4                (optional) the public IPv4 address of the Kubernetes control plane or an empty
	 *                            string if not set. This value will not be set if high availability is
	 *                            configured on the cluster.
	 * @param endpoint            the base URL of the API server
	 * @param tags                the tags that are associated with the cluster
	 * @param nodePools           the node pools that are deployed in the cluster
	 * @param maintenanceSchedule the maintenance window policy for the cluster
	 * @param autoUpgrade         {@code true} if the cluster will be automatically upgraded to new patch
	 *                            releases during its maintenance window
	 * @param status              the status of the cluster
	 * @param surgeUpgrade        {@code true} if new nodes should be deployed before destroying the outdated
	 *                            nodes
	 * @param highAvailability    {@code true} if the control plane should run in a highly available
	 *                            configuration
	 * @param canAccessRegistry   {@code true} if a container registry is integrated with the cluster
	 * @param createdAt           the time the cluster was created
	 * @param updatedAt           the time the cluster was last updated
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>any of the arguments contain leading or trailing whitespace</li>
	 *                                    <li>any of the mandatory arguments are empty</li>
	 *                                  </ul>
	 * @see ComputeClient#getDefaultVpc(Region.Id)
	 */
	public DefaultKubernetes(DefaultKubernetesClient client, Id id, String name, Region.Id region,
		KubernetesVersion version, String clusterSubnet, String serviceSubnet, Vpc.Id vpc, String ipv4,
		String endpoint, Set<String> tags, Set<NodePool> nodePools, MaintenanceSchedule maintenanceSchedule,
		boolean autoUpgrade, Status status, boolean surgeUpgrade, boolean highAvailability,
		boolean canAccessRegistry, Instant createdAt, Instant updatedAt)
	{
		requireThat(client, "client").isNotNull();
		requireThat(id, "id").isNotNull();
		requireThat(name, "name").isStripped().isNotEmpty();
		requireThat(region, "region").isNotNull();
		requireThat(version, "version").isNotNull();
		requireThat(clusterSubnet, "clusterSubnet").isNotNull();
		requireThat(serviceSubnet, "serviceSubnet").isNotNull();
		requireThat(vpc, "vpc").isNotNull();
		requireThat(ipv4, "ipv4").isNotNull();
		requireThat(endpoint, "endpoint").isNotNull();
		requireThat(tags, "tags").isNotNull();
		requireThat(nodePools, "nodePools").isNotNull();
		requireThat(maintenanceSchedule, "maintenanceSchedule").isNotNull();
		requireThat(status, "status").isNotNull();
		requireThat(createdAt, "createdAt").isNotNull();
		requireThat(updatedAt, "updatedAt").isNotNull();
		this.client = client;
		this.id = id;
		this.name = name;
		this.region = region;
		this.version = version;
		this.clusterSubnet = clusterSubnet;
		this.serviceSubnet = serviceSubnet;
		this.vpc = vpc;
		this.ipv4 = ipv4;
		this.endpoint = endpoint;
		this.tags = Set.copyOf(tags);
		this.nodePools = nodePools;
		this.maintenanceSchedule = maintenanceSchedule;
		this.autoUpgrade = autoUpgrade;
		this.status = status;
		this.surgeUpgrade = surgeUpgrade;
		this.highAvailability = highAvailability;
		this.canAccessRegistry = canAccessRegistry;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
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
	public Region.Id getRegion()
	{
		return region;
	}

	@Override
	public KubernetesVersion getVersion()
	{
		return version;
	}

	@Override
	public String getClusterSubnet()
	{
		return clusterSubnet;
	}

	@Override
	public String getServiceSubnet()
	{
		return serviceSubnet;
	}

	@Override
	public Vpc.Id getVpc()
	{
		return vpc;
	}

	@Override
	public String getIpv4()
	{
		return ipv4;
	}

	@Override
	public String getEndpoint()
	{
		return endpoint;
	}

	@Override
	public Set<String> getTags()
	{
		return tags;
	}

	@Override
	public Set<NodePool> getNodePools()
	{
		return nodePools;
	}

	@Override
	public MaintenanceSchedule getMaintenanceSchedule()
	{
		return maintenanceSchedule;
	}

	@Override
	public boolean isAutoUpgrade()
	{
		return autoUpgrade;
	}

	@Override
	public Status getStatus()
	{
		return status;
	}

	@Override
	public boolean isSurgeUpgrade()
	{
		return surgeUpgrade;
	}

	@Override
	public boolean isHighAvailability()
	{
		return highAvailability;
	}

	@Override
	public boolean canAccessRegistry()
	{
		return canAccessRegistry;
	}

	// TODO: To look up the list of clusters that have access to the registry, list all clusters and aggregate
	//  all cluster IDs that have "registry_enabled" set to true. We can then use
	// https://docs.digitalocean.com/reference/api/api-reference/#operation/kubernetes_add_registry
	// to add/remove entries.

	@Override
	public Instant getCreatedAt()
	{
		return createdAt;
	}

	@Override
	public Instant getUpdatedAt()
	{
		return updatedAt;
	}

	@Override
	public boolean matches(KubernetesCreator state)
	{
		return state.name().equals(name) && state.region().equals(region) && state.version().equals(version) &&
			state.clusterSubnet().equals(clusterSubnet) && state.serviceSubnet().equals(serviceSubnet) &&
			Objects.equals(state.vpc(), vpc) && state.tags().equals(tags) &&
			state.nodePools().equals(nodePools.stream().map(NodePool::forCreator).collect(Collectors.toSet())) &&
			state.maintenanceSchedule().equals(maintenanceSchedule) && state.autoUpgrade() == autoUpgrade &&
			state.surgeUpgrade() == surgeUpgrade && state.highAvailability() == highAvailability;
	}

	@Override
	public void update(KubernetesCreator target)
		throws IOException, InterruptedException, ResourceNotFoundException
	{
		requireThat(target.region(), "target.region").isEqualTo(region);
		requireThat(target.version(), "target.version").isEqualTo(version);
		requireThat(target.clusterSubnet(), "target.clusterSubnet").isEqualTo(clusterSubnet);
		requireThat(target.serviceSubnet(), "target.serviceSubnet").isEqualTo(serviceSubnet);
		requireThat(target.vpc(), "target.vpc").isEqualTo(vpc);
		requireThat(target.nodePools(), "target.nodePools").isEqualTo(nodePools.stream().
			map(NodePool::forCreator).collect(Collectors.toSet()));
		if (matches(target))
			return;

		JsonMapper jm = client.getJsonMapper();
		ObjectNode requestBody = jm.createObjectNode();
		requestBody.put("name", target.name());
		if (!target.tags().equals(tags))
		{
			ArrayNode tagsNode = requestBody.putArray("tags");
			for (String tag : tags)
				tagsNode.add(tag);
		}
		KubernetesParser parser = client.getParser();
		if (!target.maintenanceSchedule().equals(maintenanceSchedule))
		{
			requestBody.set("maintenance_policy", parser.maintenanceScheduleToServer(client,
				target.maintenanceSchedule()));
		}
		if (target.autoUpgrade() != autoUpgrade)
			requestBody.put("auto_upgrade", target.autoUpgrade());
		if (target.surgeUpgrade() != surgeUpgrade)
			requestBody.put("surge_upgrade", target.surgeUpgrade());
		if (target.highAvailability() != highAvailability)
			requestBody.put("ha", target.highAvailability());

		// https://docs.digitalocean.com/reference/api/api-reference/#operation/kubernetes_update_cluster
		URI uri = REST_SERVER.resolve("v2/kubernetes/clusters/" + id);
		Request request = client.createRequest(uri, requestBody).
			method(PUT);
		Response serverResponse = client.send(request);
		switch (serverResponse.getStatus())
		{
			case ACCEPTED_202 ->
			{
				// success
			}
			case NOT_FOUND_404 -> throw new ResourceNotFoundException("Kubernetes cluster: " + getId());
			default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
				"Request: " + client.toString(request));
		}
	}

	@Override
	public String getKubeConfig(Duration duration)
		throws IOException, InterruptedException, ResourceNotFoundException
	{
		// https://docs.digitalocean.com/reference/api/api-reference/#operation/kubernetes_get_kubeconfig
		URI uri = REST_SERVER.resolve("v2/kubernetes/clusters/" + id + "/kubeconfig");
		Request request = client.createRequest(uri).
			param("expiry_seconds", String.valueOf(duration.toSeconds())).
			method(GET);
		Response serverResponse = client.send(request);
		switch (serverResponse.getStatus())
		{
			case OK_200 ->
			{
				ContentResponse contentResponse = (ContentResponse) serverResponse;
				return contentResponse.getContentAsString();
			}
			case NOT_FOUND_404 -> throw new ResourceNotFoundException("Kubernetes cluster: " + getId());
			default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
				"Request: " + client.toString(request));
		}
	}

	@Override
	public Kubernetes waitFor(State state, Duration timeout)
		throws IOException, InterruptedException, TimeoutException, ResourceNotFoundException
	{
		// https://docs.digitalocean.com/reference/api/api-reference/#operation/kubernetes_get_cluster
		URI uri = REST_SERVER.resolve("v2/kubernetes/clusters/" + id);
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
				case NOT_FOUND_404 -> throw new ResourceNotFoundException("Kubernetes cluster: " + getId());
				default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
					"Request: " + client.toString(request));
			}
			ContentResponse contentResponse = (ContentResponse) serverResponse;
			JsonNode body = client.getResponseBody(contentResponse);
			KubernetesParser parser = client.getParser();
			Kubernetes newCluster = parser.kubernetesFromServer(client, body.get("kubernetes_cluster"));
			if (newCluster.getStatus().state().equals(state))
			{
				if (timeOfLastStatus != Instant.MIN)
					log.info("The status of {} is {}", name, state);
				return newCluster;
			}
			if (!timeLimit.getTimeLeft().isPositive())
				throw new TimeoutException("Operation failed after " + timeLimit.getTimeQuota());
			Instant now = Instant.now();
			if (Duration.between(timeOfLastStatus, now).compareTo(PROGRESS_FREQUENCY) >= 0)
			{
				log.info("Waiting for the status of {} to change from {} to {}", name, newCluster.getStatus().state(),
					state);
				timeOfLastStatus = now;
			}
			retryDelay.sleep();
		}
	}

	@Override
	public void destroyRecursively() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/api-reference/#operation/kubernetes_destroy_associatedResourcesDangerous
		URI uri = REST_SERVER.resolve("v2/kubernetes/clusters/" + id +
			"/destroy_with_associated_resources/dangerous");
		Request request = client.createRequest(uri).
			method(DELETE);
		Response serverResponse = client.send(request);
		switch (serverResponse.getStatus())
		{
			case NO_CONTENT_204, NOT_FOUND_404 ->
			{
				// success
			}
			default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
				"Request: " + client.toString(request));
		}
	}

	@Override
	public void waitForDestroy(Duration timeout)
		throws IOException, InterruptedException, TimeoutException
	{
		try
		{
			waitFor(State.DELETED, timeout);
		}
		catch (ResourceNotFoundException _)
		{
		}
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultKubernetes.class).
			add("id", id).
			add("name", name).
			add("region", region).
			add("version", version).
			add("clusterSubnet", clusterSubnet).
			add("serviceSubnet", serviceSubnet).
			add("vpc", vpc).
			add("ipv4", ipv4).
			add("endpoint", endpoint).
			add("tags", tags).
			add("nodePools", nodePools).
			add("maintenanceSchedule", maintenanceSchedule).
			add("autoUpgrade", autoUpgrade).
			add("status", status).
			add("surgeUpgrade", surgeUpgrade).
			add("highAvailability", highAvailability).
			add("registryEnabled", canAccessRegistry).
			add("createdAt", createdAt).
			add("updatedAt", updatedAt).
			toString();
	}
}