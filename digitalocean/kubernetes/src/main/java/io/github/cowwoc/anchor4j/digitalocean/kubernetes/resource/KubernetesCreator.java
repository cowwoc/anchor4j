package io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource;

import io.github.cowwoc.anchor4j.digitalocean.compute.client.ComputeClient;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.core.util.CreateResult;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.MaintenanceSchedule;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region.Id;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * The desired state of a Kubernetes cluster.
 */
public interface KubernetesCreator
{
	/**
	 * Returns the name of the cluster.
	 *
	 * @return the name of the cluster
	 */
	String name();

	/**
	 * Returns the region to deploy the cluster into.
	 *
	 * @return the region
	 */
	Region.Id region();

	/**
	 * Returns the kubernetes software version to deploy.
	 *
	 * @return the kubernetes software version
	 */
	KubernetesVersion version();

	/**
	 * Sets the range of IP addresses for the overlay network of the Kubernetes cluster, in CIDR notation.
	 *
	 * @param clusterSubnet the range of IP addresses in CIDR notation
	 * @return this
	 * @throws NullPointerException     if {@code clusterSubnet} is null
	 * @throws IllegalArgumentException if {@code clusterSubnet} contains leading or trailing whitespace or is
	 *                                  empty
	 */
	KubernetesCreator clusterSubnet(String clusterSubnet);

	/**
	 * Returns the range of IP addresses for the overlay network of the Kubernetes cluster, in CIDR notation.
	 *
	 * @return an empty string if unspecified
	 */
	String clusterSubnet();

	/**
	 * Sets the range of IP addresses for services running in the Kubernetes cluster, in CIDR notation.
	 *
	 * @param serviceSubnet the range of IP addresses in CIDR notation
	 * @return this
	 * @throws NullPointerException     if {@code serviceSubnet} is null
	 * @throws IllegalArgumentException if {@code serviceSubnet} contains leading or trailing whitespace or is
	 *                                  empty
	 */
	KubernetesCreator serviceSubnet(String serviceSubnet);

	/**
	 * Returns the range of IP addresses for the services running in the Kubernetes cluster, in CIDR notation.
	 *
	 * @return an empty string if unspecified
	 */
	String serviceSubnet();

	/**
	 * Returns the VPC that the cluster will use.
	 *
	 * @param vpc null to use the region's default VPC
	 * @return this
	 * @see ComputeClient#getDefaultVpc(Id)
	 */
	KubernetesCreator vpc(Vpc.Id vpc);

	/**
	 * Returns the VPC that the cluster will use.
	 *
	 * @return null if the region's default VPC will be used
	 */
	Vpc.Id vpc();

	/**
	 * Adds a tag to apply to the cluster.
	 *
	 * @param tag the tag
	 * @return this
	 * @throws NullPointerException     if {@code tag} is null
	 * @throws IllegalArgumentException if {@code tag} contains leading or trailing whitespace or is empty
	 */
	KubernetesCreator tag(String tag);

	/**
	 * Sets the tags of the cluster.
	 *
	 * @param tags the tags
	 * @return this
	 * @throws NullPointerException     if {@code tags} is null
	 * @throws IllegalArgumentException if any of the tags contains leading or trailing whitespace or are empty
	 */
	KubernetesCreator tags(Set<String> tags);

	/**
	 * Returns the tags of the cluster.
	 *
	 * @return the tags that
	 */
	Set<String> tags();

	/**
	 * Sets the node pools to deploy into the cluster.
	 *
	 * @param nodePools the node pools
	 * @return this
	 * @throws NullPointerException if {@code nodePools} is null
	 */
	KubernetesCreator nodePools(Set<NodePoolBuilder> nodePools);

	/**
	 * Returns the node pools to deploy into the cluster.
	 *
	 * @return the node pools
	 */
	Set<NodePoolBuilder> nodePools();

	/**
	 * Sets the schedule of the cluster's maintenance schedule.
	 *
	 * @param maintenanceSchedule the maintenance schedule
	 * @return this
	 * @throws NullPointerException if {@code maintenanceSchedule} is null
	 */
	KubernetesCreator maintenanceSchedule(MaintenanceSchedule maintenanceSchedule);

	/**
	 * Returns the schedule of the cluster's maintenance schedule.
	 *
	 * @return the maintenance schedule
	 */
	MaintenanceSchedule maintenanceSchedule();

	/**
	 * Determines the cluster's auto-upgrade policy. By default, this property is {@code false}.
	 *
	 * @param autoUpgrade {@code true} if the cluster will be automatically upgraded to new patch releases
	 *                    during its maintenance schedule.
	 * @return this
	 */
	KubernetesCreator autoUpgrade(boolean autoUpgrade);

	/**
	 * Determines if the cluster will be automatically upgraded to new patch releases during its maintenance
	 * schedule.
	 *
	 * @return {@code true} if the cluster will be automatically upgraded to new patch releases during its
	 * 	maintenance schedule.
	 */
	boolean autoUpgrade();

	/**
	 * Determines if new nodes should be deployed before destroying the outdated nodes. This speeds up cluster
	 * upgrades and improves their reliability. By default, this property is {@code false}.
	 *
	 * @param surgeUpgrade {@code true} if new nodes should be deployed before destroying the outdated nodes
	 * @return this
	 */
	KubernetesCreator surgeUpgrade(boolean surgeUpgrade);

	/**
	 * Determines if new nodes should be deployed before destroying the outdated nodes. This speeds up cluster
	 * upgrades and improves their reliability. By default, this property is {@code false}.
	 *
	 * @return {@code true} if new nodes should be deployed before destroying the outdated nodes
	 */
	boolean surgeUpgrade();

	/**
	 * Determines if the control plane should run in a highly available configuration. This creates multiple
	 * backup replicas of each control plane component and provides extra reliability for critical workloads.
	 * Highly available control planes incur less downtime. Once enabled, this feature cannot be disabled.
	 *
	 * @param highAvailability {@code true} if the control plane should run in a highly available configuration
	 * @return this
	 */
	KubernetesCreator highAvailability(boolean highAvailability);

	/**
	 * Determines if the control plane should run in a highly available configuration. This creates multiple
	 * backup replicas of each control plane component and provides extra reliability for critical workloads.
	 * Highly available control planes incur less downtime. Once enabled, this feature cannot be disabled.
	 *
	 * @return {@code true} if the control plane should run in a highly available configuration
	 */
	boolean highAvailability();

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
	CreateResult<Kubernetes> create() throws IOException, InterruptedException;

	/**
	 * Copies unchangeable properties from an existing cluster into this configuration. Certain properties
	 * cannot be altered once the cluster is created, and this method ensures these properties are retained.
	 *
	 * @param existingCluster the existing cluster
	 * @throws NullPointerException if {@code existingCluster} is null
	 */
	void copyUnchangeablePropertiesFrom(Kubernetes existingCluster);

	/**
	 * Builds a pool of worker nodes.
	 */
	interface NodePoolBuilder
	{
		/**
		 * Returns the type of droplets to use for cluster nodes.
		 *
		 * @return the type of droplets
		 */
		DropletType.Id dropletType();

		/**
		 * Returns the name of the node pool.
		 *
		 * @return the name
		 */
		String name();

		/**
		 * Returns the initial number of nodes to populate the pool with.
		 *
		 * @return the initial number of nodes
		 */
		int initialNumberOfNodes();

		/**
		 * Adds a tag to the pool.
		 *
		 * @param tag the tag to add
		 * @return this
		 * @throws NullPointerException     if {@code tag} is null
		 * @throws IllegalArgumentException if the tag contains leading or trailing whitespace or is empty
		 */
		NodePoolBuilder tag(String tag);

		/**
		 * Sets the tags of the node pool.
		 *
		 * @param tags the tags
		 * @return this
		 * @throws NullPointerException     if {@code tags} is null
		 * @throws IllegalArgumentException if any of the tags are null, contain leading or trailing whitespace or
		 *                                  are empty
		 */
		NodePoolBuilder tags(Collection<String> tags);

		/**
		 * Returns the pool's tags.
		 *
		 * @return the tags
		 */
		Set<String> tags();

		/**
		 * Adds a label to the pool.
		 *
		 * @param label the label to add
		 * @return this
		 * @throws NullPointerException     if {@code label} is null
		 * @throws IllegalArgumentException if the label contains leading or trailing whitespace or is empty
		 */
		NodePoolBuilder label(String label);

		/**
		 * Sets the labels of the node pool.
		 *
		 * @param labels the labels
		 * @return this
		 * @throws NullPointerException     if {@code labels} is null
		 * @throws IllegalArgumentException if any of the labels are null, contain leading or trailing whitespace
		 *                                  or are empty
		 */
		NodePoolBuilder labels(Collection<String> labels);

		/**
		 * Returns the pool's labels.
		 *
		 * @return the labels
		 */
		Set<String> labels();

		/**
		 * Adds a taint to the pool.
		 *
		 * @param taint the taint to add
		 * @return this
		 * @throws NullPointerException     if {@code taint} is null
		 * @throws IllegalArgumentException if the taint contains leading or trailing whitespace or is empty
		 */
		NodePoolBuilder taint(String taint);

		/**
		 * Sets the taints of the node pool.
		 *
		 * @param taints the taints
		 * @return this
		 * @throws NullPointerException     if {@code taints} is null
		 * @throws IllegalArgumentException if any of the taints are null, contain leading or trailing whitespace
		 *                                  or are empty
		 */
		NodePoolBuilder taints(Collection<String> taints);

		/**
		 * Returns the pool's taints.
		 *
		 * @return the taints
		 */
		Set<String> taints();

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
		NodePoolBuilder autoscale(int minNodes, int maxNodes);

		/**
		 * Indicates if the pool size should adjust automatically to meet demand.
		 *
		 * @return {@code true} if the pool size should adjust automatically to meet demand
		 */
		boolean autoScale();

		/**
		 * Returns the minimum number of nodes in the pool.
		 *
		 * @return the minimum number of nodes in the pool
		 */
		int minNodes();

		/**
		 * Returns the maximum number of nodes in the pool.
		 *
		 * @return the maximum number of nodes in the pool
		 */
		int maxNodes();
	}
}