package io.github.cowwoc.anchor4j.digitalocean.kubernetes.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.client.KubernetesClient;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.Id;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesCreator;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesCreator.NodePoolBuilder;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesParser;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesVersion;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class DefaultKubernetesClient extends AbstractDigitalOceanInternalClient
	implements KubernetesClient
{
	private final KubernetesParser parser = new KubernetesParser();

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
	public Set<Kubernetes> getKubernetes() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/api-reference/#operation/kubernetes_list_clusters
		return getElements(REST_SERVER.resolve("v2/kubernetes/clusters"), Map.of(), body ->
		{
			Set<Kubernetes> clusters = new HashSet<>();
			for (JsonNode projectNode : body.get("kubernetes_clusters"))
				clusters.add(parser.kubernetesFromServer(this, projectNode));
			return clusters;
		});
	}

	@Override
	public Kubernetes getKubernetes(Id id) throws IOException, InterruptedException
	{
		return getKubernetes(cluster -> cluster.getId().equals(id));
	}

	@Override
	public Kubernetes getKubernetes(Predicate<Kubernetes> predicate) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Kubernetes/operation/kubernetes_list_clusters
		return getElement(REST_SERVER.resolve("v2/kubernetes/clusters"), Map.of(), body ->
		{
			for (JsonNode cluster : body.get("kubernetes_clusters"))
			{
				Kubernetes candidate = parser.kubernetesFromServer(this, cluster);
				if (predicate.test(candidate))
					return candidate;
			}
			return null;
		});
	}

	@Override
	public KubernetesCreator createKubernetes(String name, Region.Id region, KubernetesVersion version,
		Set<NodePoolBuilder> nodePools)
	{
		return new DefaultKubernetesCreator(this, name, region, version, nodePools);
	}
}