package io.github.cowwoc.anchor4j.digitalocean.kubernetes.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.client.KubernetesClient;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.Id;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesCreator;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesCreator.NodePoolBuilder;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesVersion;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DefaultKubernetesClient extends AbstractDigitalOceanInternalClient
	implements KubernetesClient
{
	private final KubernetesParser parser = new KubernetesParser(this);

	/**
	 * Creates a new DefaultDatabaseClient.
	 */
	public DefaultKubernetesClient()
	{
	}

	/**
	 * Returns the parser.
	 *
	 * @return the parser
	 */
	public KubernetesParser getParser()
	{
		return parser;
	}

	@Override
	public List<Kubernetes> getKubernetesClusters() throws IOException, InterruptedException
	{
		return getKubernetesClusters(_ -> true);
	}

	@Override
	public List<Kubernetes> getKubernetesClusters(Predicate<Kubernetes> predicate)
		throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/api-reference/#operation/kubernetes_list_clusters
		return getElements(REST_SERVER.resolve("v2/kubernetes/clusters"), Map.of(), body ->
		{
			List<Kubernetes> clusters = new ArrayList<>();
			for (JsonNode projectNode : body.get("kubernetes_clusters"))
			{
				Kubernetes candidate = parser.kubernetesFromServer(projectNode);
				if (predicate.test(candidate))
					clusters.add(candidate);
			}
			return clusters;
		});
	}

	@Override
	public Kubernetes getKubernetesCluster(Id id) throws IOException, InterruptedException
	{
		return getKubernetesCluster(cluster -> cluster.getId().equals(id));
	}

	@Override
	public Kubernetes getKubernetesCluster(Predicate<Kubernetes> predicate)
		throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Kubernetes/operation/kubernetes_list_clusters
		return getElement(REST_SERVER.resolve("v2/kubernetes/clusters"), Map.of(), body ->
		{
			for (JsonNode cluster : body.get("kubernetes_clusters"))
			{
				Kubernetes candidate = parser.kubernetesFromServer(cluster);
				if (predicate.test(candidate))
					return candidate;
			}
			return null;
		});
	}

	@Override
	public KubernetesCreator createKubernetesCluster(String name, Region.Id region, KubernetesVersion version,
		Set<NodePoolBuilder> nodePools)
	{
		return new DefaultKubernetesCreator(this, name, region, version, nodePools);
	}

	@Override
	public List<Object> getResources(Predicate<? super Class<?>> typeFilter,
		Predicate<Object> resourceFilter) throws IOException, InterruptedException
	{
		ensureOpen();
		Set<Class<?>> types = Set.of(Kubernetes.class).stream().filter(typeFilter).collect(Collectors.toSet());
		if (types.isEmpty())
			return List.of();
		return List.copyOf(getKubernetesClusters(resourceFilter::test));
	}
}