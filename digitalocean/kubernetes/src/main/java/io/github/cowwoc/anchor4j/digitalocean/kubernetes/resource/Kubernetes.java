package io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource;

import io.github.cowwoc.anchor4j.core.id.StringId;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.core.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.internal.client.DefaultKubernetesCreator.DefaultNodePoolBuilder;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesCreator.NodePoolBuilder;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetTime;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A Kubernetes cluster.
 */
public interface Kubernetes
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the server-side identifier (slug)
	 * @return the type-safe identifier for the resource
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(String value)
	{
		if (value == null)
			return null;
		return new Id(value);
	}

	/**
	 * Returns the cluster's ID.
	 *
	 * @return the cluster's ID
	 */
	Id getId();

	/**
	 * Returns the cluster's name.
	 *
	 * @return the cluster's name
	 */
	String getName();

	/**
	 * Returns the region that the cluster is deployed in.
	 *
	 * @return the region
	 */
	Region.Id getRegion();

	/**
	 * Returns the version of the Kubernetes software that is deployed.
	 *
	 * @return the version of the Kubernetes software that is deployed
	 */
	KubernetesVersion getVersion();

	/**
	 * Returns the range of IP addresses for the overlay network of the Kubernetes cluster, in CIDR notation.
	 *
	 * @return the range of IP addresses for the overlay network
	 */
	String getClusterSubnet();

	/**
	 * Returns the range of IP addresses for the services running in the Kubernetes cluster, in CIDR notation.
	 *
	 * @return the range of IP addresses for the services
	 */
	String getServiceSubnet();

	/**
	 * Returns the VPC that the cluster is deployed in.
	 *
	 * @return the VPC
	 */
	Vpc.Id getVpc();

	/**
	 * Returns the public IPv4 address of the Kubernetes control plane. This value will not be set if high
	 * availability is configured on the cluster.
	 *
	 * @return an empty string if not set
	 */
	String getIpv4();

	/**
	 * Returns the base URL of the API server.
	 *
	 * @return the base URL of the API server
	 */
	String getEndpoint();

	/**
	 * Returns the tags that are associated with the cluster.
	 *
	 * @return the tags that
	 */
	Set<String> getTags();

	/**
	 * Returns the node pools that are deployed in the cluster.
	 *
	 * @return the node pools
	 */
	Set<NodePool> getNodePools();

	/**
	 * Returns the maintenance window policy for the cluster.
	 *
	 * @return the maintenance window policy
	 */
	MaintenanceSchedule getMaintenanceSchedule();

	/**
	 * Determines if the cluster will be automatically upgraded to new patch releases during its maintenance
	 * window.
	 *
	 * @return {@code true} if the cluster will be automatically upgraded
	 */
	boolean isAutoUpgrade();

	/**
	 * Returns the status of the cluster.
	 *
	 * @return the status
	 */
	Status getStatus();

	/**
	 * Determines if new nodes should be deployed before destroying the outdated nodes.
	 *
	 * @return {@code true} if new nodes should be deployed before destroying the outdated nodes
	 */
	boolean isSurgeUpgrade();

	/**
	 * Determines if the control plane should run in a highly available configuration.
	 *
	 * @return {@code true} if highly available
	 */
	boolean isHighAvailability();

	/**
	 * Determines if the cluster has access to the container registry, allowing it to push and pull images.
	 *
	 * @return {@code true} if the cluster has access
	 */
	boolean canAccessRegistry();

	/**
	 * Returns the time the cluster was created.
	 *
	 * @return the time the cluster was created
	 */
	Instant getCreatedAt();

	/**
	 * Returns the time the cluster was last updated.
	 *
	 * @return the time the cluster was last updated
	 */
	Instant getUpdatedAt();

	/**
	 * Determines if the cluster matches the desired state.
	 *
	 * @param state the desired state
	 * @return {@code true} if the droplet matches the desired state; otherwise, {@code false}
	 * @throws NullPointerException if {@code state} is null
	 */
	boolean matches(KubernetesCreator state);

	/**
	 * Updates the cluster to the desired configuration.
	 *
	 * @param target the desired state
	 * @throws NullPointerException      if {@code target} is null
	 * @throws IllegalArgumentException  if the server does not support applying any of the desired changes
	 * @throws IllegalStateException     if the client is closed
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted while waiting for a response. This can
	 *                                   happen due to shutdown signals.
	 * @throws ResourceNotFoundException if the cluster could not be found
	 * @see KubernetesCreator#copyUnchangeablePropertiesFrom(Kubernetes)
	 */
	void update(KubernetesCreator target) throws IOException, InterruptedException, ResourceNotFoundException;

	/**
	 * Downloads the kubeconfig file for this cluster. If the cluster <a
	 * href="https://docs.digitalocean.com/products/kubernetes/how-to/connect-to-cluster/#version-requirements-for-obtaining-tokens">
	 * supports</a> token-based authentication, the provided duration specifies the token's lifetime before
	 * expiration.
	 *
	 * @param duration the lifetime of the authentication token
	 * @return the {@code kubeconfig} file
	 * @throws NullPointerException      if {@code duration} is null
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted while waiting for a response. This can
	 *                                   happen due to shutdown signals.
	 * @throws ResourceNotFoundException if the cluster could not be found
	 */
	String getKubeConfig(Duration duration) throws IOException, InterruptedException, ResourceNotFoundException;

	/**
	 * Blocks until the cluster reaches the desired {@code state} or a timeout occurs.
	 *
	 * @param state   the desired state
	 * @param timeout the maximum amount of time to wait
	 * @return the updated cluster
	 * @throws NullPointerException      if {@code state} is null
	 * @throws IllegalStateException     if the client is closed
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted while waiting for a response. This can
	 *                                   happen due to shutdown signals.
	 * @throws TimeoutException          if the request times out before receiving a response. This might
	 *                                   indicate network latency or server overload.
	 * @throws ResourceNotFoundException if the cluster could not be found
	 * @see #waitForDestroy(Duration)
	 */
	Kubernetes waitFor(State state, Duration timeout)
		throws IOException, InterruptedException, TimeoutException, ResourceNotFoundException;

	/**
	 * Destroys the cluster and all of its associated resources.
	 *
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	void destroyRecursively() throws IOException, InterruptedException;

	/**
	 * Blocks until the cluster is destroyed.
	 *
	 * @param timeout the maximum amount of time to wait
	 * @throws NullPointerException if {@code state} is null
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted while waiting for a response. This can happen
	 *                              due to shutdown signals.
	 * @throws TimeoutException     if the operation times out before the cluster reaches the desired status.
	 *                              This might indicate network latency or server overload.
	 */
	void waitForDestroy(Duration timeout) throws IOException, InterruptedException, TimeoutException;

	/**
	 * A pool of worker nodes.
	 *
	 * @param id                   the ID of the node pool
	 * @param name                 the name of the node pool
	 * @param dropletType          the type of droplets to use for cluster nodes
	 * @param initialNumberOfNodes the initial number of nodes to populate the pool with
	 * @param tags                 the pool's tags
	 * @param labels               the pool's labels
	 * @param taints               the pool's taints
	 * @param autoScale            {@code true} if the pool size should adjust automatically to meet demand
	 * @param minNodes             the minimum number of nodes in the pool
	 * @param maxNodes             the maximum number of nodes in the pool
	 * @param nodes                the nodes in the pool
	 */
	record NodePool(String id, String name, DropletType.Id dropletType, int initialNumberOfNodes,
	                Set<String> tags, Set<String> labels, Set<String> taints, boolean autoScale, int minNodes,
	                int maxNodes, Set<Node> nodes)
	{
		/**
		 * @param id                   the ID of the node pool
		 * @param name                 the name of the node pool
		 * @param dropletType          the type of droplets to use for cluster nodes
		 * @param initialNumberOfNodes the initial number of nodes to populate the pool with
		 * @param tags                 the pool's tags
		 * @param labels               the pool's labels
		 * @param taints               the pool's taints
		 * @param autoScale            {@code true} if the pool size should adjust automatically to meet demand
		 * @param minNodes             the minimum number of nodes in the pool
		 * @param maxNodes             the maximum number of nodes in the pool
		 * @param nodes                the nodes in the pool
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if:
		 *                                  <ul>
		 *                                    <li>any of the arguments contain leading or trailing whitespace or
		 *                                    are empty.</li>
		 *                                    <li>{@code initialNumberOfNodes} is negative or zero.</li>
		 *                                    <li>{@code initialNumberOfNodes} is less than
		 *                                    {@code minNodes}.</li>
		 *                                    <li>{@code initialNumberOfNodes} is greater than
		 *                                    {@code maxNodes}.</li>
		 *                                    <li>{@code minNodes} is greater than {@code maxNodes}.</li>
		 *                                  </ul>
		 */
		public NodePool(String id, String name, DropletType.Id dropletType, int initialNumberOfNodes,
			Set<String> tags, Set<String> labels, Set<String> taints, boolean autoScale, int minNodes, int maxNodes,
			Set<Node> nodes)
		{
			requireThat(id, "id").isStripped().isNotEmpty();
			requireThat(name, "name").isStripped().isNotEmpty();
			requireThat(dropletType, "dropletType").isNotNull();
			requireThat(initialNumberOfNodes, "initialNumberOfNodes").isPositive();
			requireThat(tags, "tags").isNotNull();
			requireThat(labels, "labels").isNotNull();
			requireThat(taints, "taints").isNotNull();
			requireThat(minNodes, "minNodes").isPositive();
			requireThat(maxNodes, "maxNodes").isPositive();

			requireThat(minNodes, "minNodes").isLessThanOrEqualTo(maxNodes, "maxNodes");
			requireThat(initialNumberOfNodes, "initialNumberOfNodes").
				isBetween(minNodes, true, maxNodes, true);

			requireThat(nodes, "nodes").isNotNull();

			this.id = id;
			this.name = name;
			this.dropletType = dropletType;
			this.initialNumberOfNodes = initialNumberOfNodes;
			this.tags = Set.copyOf(tags);
			this.labels = Set.copyOf(labels);
			this.taints = Set.copyOf(taints);
			this.autoScale = autoScale;
			this.minNodes = initialNumberOfNodes;
			this.maxNodes = initialNumberOfNodes;
			this.nodes = Set.copyOf(nodes);
		}

		/**
		 * Converts this object to a type that is accepted by {@code KubernetesCreator}.
		 *
		 * @return the {@code KubernetesCreator.NodePool}
		 */
		public NodePoolBuilder forCreator()
		{
			NodePoolBuilder creator = new DefaultNodePoolBuilder(name,
				dropletType, initialNumberOfNodes).
				tags(tags).
				labels(labels).
				taints(taints);
			if (autoScale)
				creator.autoscale(minNodes, maxNodes);
			return creator;
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(NodePool.class).
				add("id", id).
				add("name", name).
				add("type", dropletType).
				add("count", initialNumberOfNodes).
				add("tags", tags).
				add("labels", labels).
				add("taints", taints).
				add("autoScale", autoScale).
				add("minNodes", minNodes).
				add("maxNodes", maxNodes).
				toString();
		}
	}

	/**
	 * A worker node.
	 *
	 * @param id        the ID of the node
	 * @param name      the name of the node or an empty string if the droplet is still provisioning
	 * @param status    the status of the node
	 * @param dropletId the droplet that the node is running on or an empty string if the droplet is still
	 *                  provisioning
	 * @param createdAt the time that the node was created
	 * @param updatedAt the last time that the node was updated
	 */
	record Node(String id, String name, NodeStatus status, String dropletId, Instant createdAt,
	            Instant updatedAt)
	{
		/**
		 * Creates a new node.
		 *
		 * @param id        the ID of the node
		 * @param name      the name of the node or an empty string if the droplet is still provisioning
		 * @param status    the status of the node
		 * @param dropletId the droplet that the node is running on or an empty string if the droplet is still
		 *                  provisioning
		 * @param createdAt the time that the node was created
		 * @param updatedAt the last time that the node was updated
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if any of the arguments contain leading or trailing whitespace or is
		 *                                  empty
		 */
		public Node
		{
			requireThat(id, "id").isStripped().isNotEmpty();
			requireThat(name, "name").isStripped();
			requireThat(status, "status").isNotNull();
			requireThat(dropletId, "dropletId").isStripped();
			requireThat(createdAt, "createdAt").isNotNull();
			requireThat(updatedAt, "updatedAt").isNotNull();

		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(Node.class).
				add("id", id).
				add("name", name).
				add("state", status).
				add("dropletId", dropletId).
				add("createdAt", createdAt).
				add("updatedAt", updatedAt).
				toString();
		}
	}

	/**
	 * The status of a node.
	 *
	 * @param state   the state of the node
	 * @param message (optional) a message providing additional information about the node state, or an empty
	 *                string if absent
	 */
	record NodeStatus(NodeState state, String message)
	{
		/**
		 * Creates a new node status.
		 *
		 * @param state   the state of the node
		 * @param message (optional) a message providing additional information about the node state, or an empty
		 *                string if absent
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code message} contains leading or trailing whitespace or is
		 *                                  empty
		 */
		public NodeStatus
		{
			requireThat(state, "state").isNotNull();
			requireThat(message, "message").isStripped();
		}

		/**
		 * Returns the state of a node.
		 *
		 * @return the state
		 */
		@Override
		public NodeState state()
		{
			return state;
		}

		/**
		 * Returns an optional message providing additional information about the node state.
		 *
		 * @return an empty string if absent
		 */
		@Override
		public String message()
		{
			return message;
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(NodeState.class).
				add("state", state).
				add("message", message).
				toString();
		}
	}

	/**
	 * The state of a node.
	 */
	enum NodeState
	{
		/**
		 * The node is being configured.
		 */
		PROVISIONING,
		/**
		 * The node is up and running.
		 */
		RUNNING,
		/**
		 * The node is evicting all pods to other nodes.
		 */
		DRAINING,
		/**
		 * The node is being deleted.
		 */
		DELETING
	}

	/**
	 * The schedule for when maintenance activities may be performed on the cluster.
	 *
	 * @param startTime the start time when maintenance may take place
	 * @param day       the day of the week when maintenance may take place
	 */
	record MaintenanceSchedule(OffsetTime startTime, DayOfWeek day)
	{
		/**
		 * Creates a new schedule.
		 *
		 * @param startTime the start time when maintenance may take place
		 * @param day       the day of the week when maintenance may take place; {@code null} if maintenance can
		 *                  occur on any day.
		 * @throws NullPointerException     if {@code client} or {@code startTime} are null
		 * @throws IllegalArgumentException if {@code startTime} contains a non-zero second or nano component
		 */
		public MaintenanceSchedule
		{
			requireThat(startTime, "startTime").isNotNull();
			requireThat(startTime.getSecond(), "hour.getSecond()").isZero();
			requireThat(startTime.getNano(), "hour.getNano()").isZero();
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(MaintenanceSchedule.class).
				add("startTime", startTime).
				add("day", day).
				toString();
		}
	}

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value a server-side identifier
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
		 */
		private Id(String value)
		{
			super(value);
		}
	}

	/**
	 * A cluster's state.
	 */
	enum State
	{
		/**
		 * The cluster is launching.
		 */
		PROVISIONING,
		/**
		 * The cluster is up and running.
		 */
		RUNNING,
		/**
		 * <ul>
		 *   <li>One or more nodes in the cluster have become unresponsive or failed.</li>
		 *   <li>The cluster has run out of resources (CPU, memory, etc.).</li>
		 * </ul>
		 */
		DEGRADED,
		/**
		 * <ul>
		 *   <li>The cluster has experiencing resource exhaustion.</li>
		 *   <li>The scheduler was unable to find suitable nodes for pod placement.</li>
		 *   <li>Issues with individual pods, such as failed deployments or evictions, can contribute to the
		 *   overall cluster error status.</li>
		 * </ul>
		 */
		ERROR,
		/**
		 * The cluster has been deleted.
		 */
		DELETED,
		/**
		 * The cluster is being upgraded.
		 */
		UPGRADING,
		/**
		 * The cluster is being deleted.
		 */
		DELETING
	}

	/**
	 * The status of a cluster.
	 *
	 * @param state   the state of the cluster
	 * @param message an optional message providing additional information about the state, or an empty string
	 *                if absent
	 */
	record Status(State state, String message)
	{
		/**
		 * Creates a new status.
		 *
		 * @param state   the state of the cluster
		 * @param message an optional message providing additional information about the state, or an empty string
		 *                if absent
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code message} contains leading or trailing whitespace or is
		 *                                  empty
		 */
		public Status
		{
			requireThat(state, "state").isNotNull();
			requireThat(message, "message").isStripped();
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(NodeState.class).
				add("state", state).
				add("message", message).
				toString();
		}
	}
}