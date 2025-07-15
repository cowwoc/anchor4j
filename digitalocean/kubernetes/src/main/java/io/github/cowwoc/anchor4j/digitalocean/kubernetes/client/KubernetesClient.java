package io.github.cowwoc.anchor4j.digitalocean.kubernetes.client;

import io.github.cowwoc.anchor4j.digitalocean.core.client.DigitalOceanClient;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.internal.client.DefaultKubernetesClient;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.Id;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesCreator;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesCreator.NodePoolBuilder;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesVersion;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A DigitalOcean Kubernetes client.
 */
public interface KubernetesClient extends DigitalOceanClient
{
	/**
	 * Returns a client.
	 *
	 * @return the client
	 * @throws IOException if an I/O error occurs while building the client
	 */
	static KubernetesClient build() throws IOException
	{
		return new DefaultKubernetesClient();
	}

	/**
	 * Returns all the Kubernetes clusters.
	 *
	 * @return an empty list if no match is found
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<Kubernetes> getKubernetesClusters() throws IOException, InterruptedException;

	/**
	 * Returns the first Kubernetes cluster that matches a predicate.
	 *
	 * @param predicate the predicate
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code predicate} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<Kubernetes> getKubernetesClusters(Predicate<Kubernetes> predicate)
		throws IOException, InterruptedException;

	/**
	 * Looks up a Kubernetes cluster by its ID.
	 *
	 * @param id the ID
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code id} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Kubernetes getKubernetesCluster(Id id) throws IOException, InterruptedException;

	/**
	 * Returns the first Kubernetes cluster that matches a predicate.
	 *
	 * @param predicate the predicate
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code predicate} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Kubernetes getKubernetesCluster(Predicate<Kubernetes> predicate) throws IOException, InterruptedException;

	/**
	 * Creates a new Kubernetes cluster.
	 *
	 * @param name      the name of the node pool
	 * @param region    the region to deploy the cluster into
	 * @param version   the version of Kubernetes software
	 * @param nodePools the node pools that are deployed in the cluster
	 * @return a new cluster creator
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if any of the arguments contain leading or trailing whitespace or are
	 *                                  empty
	 */
	@CheckReturnValue
	KubernetesCreator createKubernetesCluster(String name, Region.Id region, KubernetesVersion version,
		Set<NodePoolBuilder> nodePools);
}