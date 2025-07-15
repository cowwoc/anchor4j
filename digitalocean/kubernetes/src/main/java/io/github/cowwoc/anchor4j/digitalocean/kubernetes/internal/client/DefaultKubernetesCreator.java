package io.github.cowwoc.anchor4j.digitalocean.kubernetes.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.client.ComputeClient;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.core.util.CreateResult;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.MaintenanceSchedule;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesCreator;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesVersion;
import io.github.cowwoc.anchor4j.digitalocean.network.internal.resource.NetworkParser;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region.Id;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient.REST_SERVER;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static org.eclipse.jetty.http.HttpMethod.POST;
import static org.eclipse.jetty.http.HttpStatus.CREATED_201;
import static org.eclipse.jetty.http.HttpStatus.UNPROCESSABLE_ENTITY_422;

public final class DefaultKubernetesCreator implements KubernetesCreator
{
	private final DefaultKubernetesClient client;
	private final String name;
	private final Region.Id region;
	private final KubernetesVersion version;
	private final Set<String> tags = new LinkedHashSet<>();
	private final Set<NodePoolBuilder> nodePools;
	private final NetworkParser networkParser;
	private String clusterSubnet = "";
	private String serviceSubnet = "";
	private Vpc.Id vpc;
	private MaintenanceSchedule maintenanceSchedule;
	private boolean autoUpgrade;
	private boolean surgeUpgrade;
	private boolean highAvailability;

	/**
	 * Creates a new instance.
	 *
	 * @param client    the client configuration
	 * @param name      the name of the cluster. Names are case-insensitive.
	 * @param region    the region to deploy the cluster into
	 * @param version   the version of Kubernetes software to deploy
	 * @param nodePools the node pools to deploy into the cluster
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>any of the arguments contain leading or trailing whitespace or
	 *                                    are empty.</li>
	 *                                    <li>{@code nodePools} is empty.</li>
	 *                                  </ul>
	 */
	public DefaultKubernetesCreator(DefaultKubernetesClient client, String name, Region.Id region,
		KubernetesVersion version, Set<NodePoolBuilder> nodePools)
	{
		requireThat(client, "client").isNotNull();
		requireThat(name, "name").isStripped().isNotEmpty();
		requireThat(region, "region").isNotNull();
		requireThat(version, "version").isNotNull();
		requireThat(nodePools, "nodePools").isNotEmpty();
		this.client = client;
		this.name = name;
		this.region = region;
		this.version = version;
		this.nodePools = new HashSet<>(nodePools);
		this.networkParser = new NetworkParser(client);
	}

	/**
	 * Returns the name of the cluster.
	 *
	 * @return the name of the cluster
	 */
	public String name()
	{
		return name;
	}

	/**
	 * Returns the region to deploy the cluster into.
	 *
	 * @return the region
	 */
	public Region.Id region()
	{
		return region;
	}

	/**
	 * Returns the kubernetes software version to deploy.
	 *
	 * @return the kubernetes software version
	 */
	public KubernetesVersion version()
	{
		return version;
	}

	/**
	 * Sets the range of IP addresses for the overlay network of the Kubernetes cluster, in CIDR notation.
	 *
	 * @param clusterSubnet the range of IP addresses in CIDR notation
	 * @return this
	 * @throws NullPointerException     if {@code clusterSubnet} is null
	 * @throws IllegalArgumentException if {@code clusterSubnet} contains leading or trailing whitespace or is
	 *                                  empty
	 */
	public DefaultKubernetesCreator clusterSubnet(String clusterSubnet)
	{
		requireThat(clusterSubnet, "clusterSubnet").isStripped().isNotEmpty();
		this.clusterSubnet = clusterSubnet;
		return this;
	}

	/**
	 * Returns the range of IP addresses for the overlay network of the Kubernetes cluster, in CIDR notation.
	 *
	 * @return an empty string if unspecified
	 */
	public String clusterSubnet()
	{
		return clusterSubnet;
	}

	/**
	 * Sets the range of IP addresses for services running in the Kubernetes cluster, in CIDR notation.
	 *
	 * @param serviceSubnet the range of IP addresses in CIDR notation
	 * @return this
	 * @throws NullPointerException     if {@code serviceSubnet} is null
	 * @throws IllegalArgumentException if {@code serviceSubnet} contains leading or trailing whitespace or is
	 *                                  empty
	 */
	public DefaultKubernetesCreator serviceSubnet(String serviceSubnet)
	{
		requireThat(serviceSubnet, "serviceSubnet").isStripped().isNotEmpty();
		this.serviceSubnet = serviceSubnet;
		return this;
	}

	/**
	 * Returns the range of IP addresses for the services running in the Kubernetes cluster, in CIDR notation.
	 *
	 * @return an empty string if unspecified
	 */
	public String serviceSubnet()
	{
		return serviceSubnet;
	}

	/**
	 * Returns the VPC that the cluster will use.
	 *
	 * @param vpc null to use the region's default VPC
	 * @return this
	 * @see ComputeClient#getDefaultVpc(Id)
	 */
	public DefaultKubernetesCreator vpc(Vpc.Id vpc)
	{
		this.vpc = vpc;
		return this;
	}

	/**
	 * Returns the VPC that the cluster will use.
	 *
	 * @return null if the region's default VPC will be used
	 */
	public Vpc.Id vpc()
	{
		return vpc;
	}

	/**
	 * Adds a tag to apply to the cluster.
	 *
	 * @param tag the tag
	 * @return this
	 * @throws NullPointerException     if {@code tag} is null
	 * @throws IllegalArgumentException if {@code tag} contains leading or trailing whitespace or is empty
	 */
	public DefaultKubernetesCreator tag(String tag)
	{
		requireThat(tag, "tag").isStripped().isNotEmpty();
		tags.add(tag);
		return this;
	}

	/**
	 * Sets the tags of the cluster.
	 *
	 * @param tags the tags
	 * @return this
	 * @throws NullPointerException     if {@code tags} is null
	 * @throws IllegalArgumentException if any of the tags contains leading or trailing whitespace or are empty
	 */
	public DefaultKubernetesCreator tags(Set<String> tags)
	{
		requireThat(tags, "tags").isNotNull();
		this.tags.clear();
		for (String tag : tags)
		{
			requireThat(tag, "tag").withContext(tags, "tags").isStripped().isNotEmpty();
			this.tags.add(tag);
		}
		return this;
	}

	/**
	 * Returns the tags of the cluster.
	 *
	 * @return the tags that
	 */
	public Set<String> tags()
	{
		return tags;
	}

	/**
	 * Sets the node pools to deploy into the cluster.
	 *
	 * @param nodePools the node pools
	 * @return this
	 * @throws NullPointerException if {@code nodePools} is null
	 */
	public DefaultKubernetesCreator nodePools(Set<NodePoolBuilder> nodePools)
	{
		requireThat(nodePools, "nodePools").isNotNull();
		this.nodePools.clear();
		this.nodePools.addAll(nodePools);
		return this;
	}

	/**
	 * Returns the node pools to deploy into the cluster.
	 *
	 * @return the node pools
	 */
	public Set<NodePoolBuilder> nodePools()
	{
		return nodePools;
	}

	/**
	 * Sets the schedule of the cluster's maintenance schedule.
	 *
	 * @param maintenanceSchedule the maintenance schedule
	 * @return this
	 * @throws NullPointerException if {@code maintenanceSchedule} is null
	 */
	public DefaultKubernetesCreator maintenanceSchedule(MaintenanceSchedule maintenanceSchedule)
	{
		requireThat(maintenanceSchedule, "maintenanceSchedule").isNotNull();
		this.maintenanceSchedule = maintenanceSchedule;
		return this;
	}

	/**
	 * Returns the schedule of the cluster's maintenance schedule.
	 *
	 * @return the maintenance schedule
	 */
	public MaintenanceSchedule maintenanceSchedule()
	{
		return maintenanceSchedule;
	}

	/**
	 * Determines the cluster's auto-upgrade policy. By default, this property is {@code false}.
	 *
	 * @param autoUpgrade {@code true} if the cluster will be automatically upgraded to new patch releases
	 *                    during its maintenance schedule.
	 * @return this
	 */
	public DefaultKubernetesCreator autoUpgrade(boolean autoUpgrade)
	{
		this.autoUpgrade = autoUpgrade;
		return this;
	}

	/**
	 * Determines if the cluster will be automatically upgraded to new patch releases during its maintenance
	 * schedule.
	 *
	 * @return {@code true} if the cluster will be automatically upgraded to new patch releases during its
	 * 	maintenance schedule.
	 */
	public boolean autoUpgrade()
	{
		return autoUpgrade;
	}

	/**
	 * Determines if new nodes should be deployed before destroying the outdated nodes. This speeds up cluster
	 * upgrades and improves their reliability. By default, this property is {@code false}.
	 *
	 * @param surgeUpgrade {@code true} if new nodes should be deployed before destroying the outdated nodes
	 * @return this
	 */
	public DefaultKubernetesCreator surgeUpgrade(boolean surgeUpgrade)
	{
		this.surgeUpgrade = surgeUpgrade;
		return this;
	}

	/**
	 * Determines if new nodes should be deployed before destroying the outdated nodes. This speeds up cluster
	 * upgrades and improves their reliability. By default, this property is {@code false}.
	 *
	 * @return {@code true} if new nodes should be deployed before destroying the outdated nodes
	 */
	public boolean surgeUpgrade()
	{
		return surgeUpgrade;
	}

	/**
	 * Determines if the control plane should run in a highly available configuration. This creates multiple
	 * backup replicas of each control plane component and provides extra reliability for critical workloads.
	 * Highly available control planes incur less downtime. Once enabled, this feature cannot be disabled.
	 *
	 * @param highAvailability {@code true} if the control plane should run in a highly available configuration
	 * @return this
	 */
	public DefaultKubernetesCreator highAvailability(boolean highAvailability)
	{
		this.highAvailability = highAvailability;
		return this;
	}

	/**
	 * Determines if the control plane should run in a highly available configuration. This creates multiple
	 * backup replicas of each control plane component and provides extra reliability for critical workloads.
	 * Highly available control planes incur less downtime. Once enabled, this feature cannot be disabled.
	 *
	 * @return {@code true} if the control plane should run in a highly available configuration
	 */
	public boolean highAvailability()
	{
		return highAvailability;
	}

	/**
	 * Creates a new cluster.
	 *
	 * @return the new or conflicting cluster
	 * @throws IllegalArgumentException if a cluster with this name already exists
	 * @throws IllegalStateException    if the client is closed
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	public CreateResult<Kubernetes> create() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/api-reference/#operation/kubernetes_create_cluster
		JsonMapper jm = client.getJsonMapper();
		KubernetesParser k8sParser = client.getParser();
		ObjectNode requestBody = jm.createObjectNode().
			put("name", name).
			put("region", networkParser.regionIdToServer(region)).
			put("version", k8sParser.kubernetesVersionToServer(version));
		ArrayNode nodePoolsNode = requestBody.putArray("node_pools");
		for (NodePoolBuilder pool : nodePools)
			nodePoolsNode.add(k8sParser.nodePoolToServer(pool));
		if (!clusterSubnet.isEmpty())
			requestBody.put("clusterSubnet", clusterSubnet);
		if (!serviceSubnet.isEmpty())
			requestBody.put("serviceSubnet", serviceSubnet);
		if (vpc != null)
			requestBody.put("vpc_uuid", vpc.getValue());
		if (maintenanceSchedule != null)
			requestBody.set("maintenance_policy",
				k8sParser.maintenanceScheduleToServer(maintenanceSchedule));
		if (autoUpgrade)
			requestBody.put("auto_upgrade", true);
		if (surgeUpgrade)
			requestBody.put("surge_upgrade", true);
		if (highAvailability)
			requestBody.put("ha", true);
		Request request = client.createRequest(REST_SERVER.resolve("v2/kubernetes/clusters"), requestBody).
			method(POST);
		Response serverResponse = client.send(request);
		return switch (serverResponse.getStatus())
		{
			case CREATED_201 ->
			{
				ContentResponse contentResponse = (ContentResponse) serverResponse;
				JsonNode body = client.getResponseBody(contentResponse);
				yield CreateResult.created(k8sParser.kubernetesFromServer(body.get("kubernetes_cluster")));
			}
			case UNPROCESSABLE_ENTITY_422 ->
			{
				// Example: "a cluster with this name already exists"
				ContentResponse contentResponse = (ContentResponse) serverResponse;
				JsonNode json = client.getResponseBody(contentResponse);
				String message = json.get("message").textValue();
				if (message.equals("a cluster with this name already exists"))
				{
					Kubernetes conflict = client.getKubernetesCluster(cluster -> cluster.getName().equals(name));
					if (conflict != null)
						yield CreateResult.conflictedWith(conflict);
				}
				throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
					"Request: " + client.toString(request));
			}
			default -> throw new AssertionError("Unexpected response: " + client.toString(serverResponse) + "\n" +
				"Request: " + client.toString(request));
		};
	}

	/**
	 * Copies unchangeable properties from an existing cluster into this configuration. Certain properties
	 * cannot be altered once the cluster is created, and this method ensures these properties are retained.
	 *
	 * @param existingCluster the existing cluster
	 * @throws NullPointerException if {@code existingCluster} is null
	 */
	public void copyUnchangeablePropertiesFrom(Kubernetes existingCluster)
	{
		clusterSubnet(existingCluster.getClusterSubnet());
		serviceSubnet(existingCluster.getServiceSubnet());
		vpc(existingCluster.getVpc());
		nodePools(existingCluster.getNodePools().stream().map(Kubernetes.NodePool::forCreator).
			collect(Collectors.toSet()));
		// Add auto-generated tags
		tag("k8s");
		tag("k8s:" + existingCluster.getId().getValue());
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultKubernetesCreator.class).
			add("name", name).
			add("region", region).
			add("version", version).
			add("clusterSubnet", clusterSubnet).
			add("serviceSubnet", serviceSubnet).
			add("vpc", vpc).
			add("tags", tags).
			add("nodePools", nodePools).
			add("maintenanceSchedule", maintenanceSchedule).
			add("autoUpgrade", autoUpgrade).
			add("surgeUpgrade", surgeUpgrade).
			add("highAvailability", highAvailability).
			toString();
	}

	public static final class DefaultNodePoolBuilder implements NodePoolBuilder
	{
		private final String name;
		private final DropletType.Id dropletType;
		private final int initialNumberOfNodes;
		private final Set<String> tags = new LinkedHashSet<>();
		private final Set<String> labels = new LinkedHashSet<>();
		private final Set<String> taints = new LinkedHashSet<>();
		private boolean autoScale;
		private int minNodes;
		private int maxNodes;

		/**
		 * Creates a new node pool.
		 *
		 * @param name                 the name of the node pool
		 * @param dropletType          the type of droplets to use for cluster nodes
		 * @param initialNumberOfNodes the initial number of nodes to populate the pool with
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if:
		 *                                  <ul>
		 *                                    <li>any of the arguments contain leading or trailing whitespace or
		 *                                    are empty.</li>
		 *                                    <li>{@code initialNumberOfNodes} is negative or zero.</li>
		 *                                  </ul>
		 */
		public DefaultNodePoolBuilder(String name, DropletType.Id dropletType, int initialNumberOfNodes)
		{
			requireThat(name, "name").isStripped().isNotEmpty();
			requireThat(dropletType, "dropletType").isNotNull();
			requireThat(initialNumberOfNodes, "initialNumberOfNodes").isPositive();

			this.name = name;
			this.dropletType = dropletType;
			this.initialNumberOfNodes = initialNumberOfNodes;
		}

		/**
		 * Returns the type of droplets to use for cluster nodes.
		 *
		 * @return the type of droplets to use for cluster nodes
		 */
		public DropletType.Id dropletType()
		{
			return dropletType;
		}

		/**
		 * Returns the name of the node pool.
		 *
		 * @return the name of the node pool
		 */
		public String name()
		{
			return name;
		}

		/**
		 * Returns the initial number of nodes to populate the pool with.
		 *
		 * @return the initial number of nodes
		 */
		public int initialNumberOfNodes()
		{
			return initialNumberOfNodes;
		}

		/**
		 * Adds a tag to the pool.
		 *
		 * @param tag the tag to add
		 * @return this
		 * @throws NullPointerException     if {@code tag} is null
		 * @throws IllegalArgumentException if the tag contains leading or trailing whitespace or is empty
		 */
		public NodePoolBuilder tag(String tag)
		{
			requireThat(tag, "tag").isStripped().isNotEmpty();
			this.tags.add(tag);
			return this;
		}

		/**
		 * Sets the tags of the node pool.
		 *
		 * @param tags the tags
		 * @return this
		 * @throws NullPointerException     if {@code tags} is null
		 * @throws IllegalArgumentException if any of the tags are null, contain leading or trailing whitespace or
		 *                                  are empty
		 */
		public NodePoolBuilder tags(Collection<String> tags)
		{
			requireThat(tags, "tags").isNotNull().doesNotContain(null);
			this.tags.clear();
			for (String tag : tags)
			{
				requireThat(tag, "tag").withContext(tags, "tags").isStripped().isNotEmpty();
				this.tags.add(tag);
			}
			return this;
		}

		/**
		 * Returns the pool's tags.
		 *
		 * @return the tags
		 */
		public Set<String> tags()
		{
			return Set.copyOf(tags);
		}

		/**
		 * Adds a label to the pool.
		 *
		 * @param label the label to add
		 * @return this
		 * @throws NullPointerException     if {@code label} is null
		 * @throws IllegalArgumentException if the label contains leading or trailing whitespace or is empty
		 */
		public NodePoolBuilder label(String label)
		{
			requireThat(label, "label").isStripped().isNotEmpty();
			this.labels.add(label);
			return this;
		}

		/**
		 * Sets the labels of the node pool.
		 *
		 * @param labels the labels
		 * @return this
		 * @throws NullPointerException     if {@code labels} is null
		 * @throws IllegalArgumentException if any of the labels are null, contain leading or trailing whitespace
		 *                                  or are empty
		 */
		public NodePoolBuilder labels(Collection<String> labels)
		{
			requireThat(labels, "labels").isNotNull().doesNotContain(null);
			this.labels.clear();
			for (String label : labels)
			{
				requireThat(label, "label").withContext(labels, "labels").isStripped().isNotEmpty();
				this.labels.add(label);
			}
			return this;
		}

		/**
		 * Returns the pool's labels.
		 *
		 * @return the labels
		 */
		public Set<String> labels()
		{
			return Set.copyOf(labels);
		}

		/**
		 * Adds a taint to the pool.
		 *
		 * @param taint the taint to add
		 * @return this
		 * @throws NullPointerException     if {@code taint} is null
		 * @throws IllegalArgumentException if the taint contains leading or trailing whitespace or is empty
		 */
		public NodePoolBuilder taint(String taint)
		{
			requireThat(taint, "taint").isStripped().isNotEmpty();
			this.taints.add(taint);
			return this;
		}

		/**
		 * Sets the taints of the node pool.
		 *
		 * @param taints the taints
		 * @return this
		 * @throws NullPointerException     if {@code taints} is null
		 * @throws IllegalArgumentException if any of the taints are null, contain leading or trailing whitespace
		 *                                  or are empty
		 */
		public NodePoolBuilder taints(Collection<String> taints)
		{
			requireThat(taints, "taints").isNotNull().doesNotContain(null);
			this.taints.clear();
			for (String taint : taints)
			{
				requireThat(taint, "taint").withContext(taints, "taints").isStripped().isNotEmpty();
				this.taints.add(taint);
			}
			return this;
		}

		/**
		 * Returns the pool's taints.
		 *
		 * @return the taints
		 */
		public Set<String> taints()
		{
			return Set.copyOf(taints);
		}

		/**
		 * Configures the pool size to adjust automatically to meet demand.
		 *
		 * @param minNodes the minimum number of nodes in the pool
		 * @param maxNodes the maximum number of nodes in the pool
		 * @return this
		 * @throws IllegalArgumentException if:
		 *                                  <ul>
		 *                                    <li>{@code minNodes} is greater than
		 *                                    {@link #initialNumberOfNodes()}</li>
		 *                                    <li>{@code maxNodes} is less than
		 *                                    {@link #initialNumberOfNodes()}</li>
		 *                                  </ul>
		 */
		public NodePoolBuilder autoscale(int minNodes, int maxNodes)
		{
			requireThat(minNodes, "minNodes").isLessThanOrEqualTo(maxNodes, "maxNodes");
			this.autoScale = true;
			this.minNodes = minNodes;
			this.maxNodes = maxNodes;
			return this;
		}

		/**
		 * Indicates if the pool size should adjust automatically to meet demand.
		 *
		 * @return {@code true} if the pool size should adjust automatically to meet demand
		 */
		public boolean autoScale()
		{
			return autoScale;
		}

		/**
		 * Returns the minimum number of nodes in the pool.
		 *
		 * @return the minimum number of nodes in the pool
		 */
		public int minNodes()
		{
			return minNodes;
		}

		/**
		 * Returns the maximum number of nodes in the pool.
		 *
		 * @return the maximum number of nodes in the pool
		 */
		public int maxNodes()
		{
			return maxNodes;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(name, dropletType, initialNumberOfNodes, tags, labels, taints, autoScale, minNodes,
				maxNodes);
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof NodePoolBuilder other && other.name().equals(name) &&
				other.dropletType().equals(dropletType) && other.initialNumberOfNodes() == initialNumberOfNodes &&
				other.tags().equals(tags) && other.labels().equals(labels) && other.taints().equals(taints) &&
				other.autoScale() == autoScale && other.minNodes() == minNodes && other.maxNodes() == maxNodes;
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(NodePoolBuilder.class).
				add("name", name).
				add("type", dropletType).
				add("initialNumberOfNodes", initialNumberOfNodes).
				add("tags", tags).
				add("labels", labels).
				add("taints", taints).
				add("autoScale", autoScale).
				add("minNodes", minNodes).
				add("maxNodes", maxNodes).
				toString();
		}
	}
}