package io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.cowwoc.anchor4j.core.internal.resource.AbstractParser;
import io.github.cowwoc.anchor4j.core.internal.util.Strings;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.resource.ComputeParser;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.client.KubernetesClient;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.internal.client.DefaultKubernetes;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.internal.client.DefaultKubernetesClient;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.Id;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.MaintenanceSchedule;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.Node;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.NodePool;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.NodeState;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.NodeStatus;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.State;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.Kubernetes.Status;
import io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource.KubernetesCreator.NodePoolBuilder;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Parses server responses.
 */
public final class KubernetesParser extends AbstractParser
{
	private final ComputeParser computeParser = new ComputeParser();

	/**
	 * Converts a Kubernetes cluster from its server representation.
	 *
	 * @param client the client configuration
	 * @param json   the JSON representation
	 * @return the cluster
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 * @throws IllegalStateException    if the client is closed
	 */
	public Kubernetes kubernetesFromServer(DefaultKubernetesClient client, JsonNode json)
	{
		Id id = Kubernetes.id(json.get("id").textValue());
		String name = json.get("name").textValue();
		Region.Id region = computeParser.regionIdFromServer(json.get("region"));
		KubernetesVersion version = KubernetesVersion.fromJson(json.get("version"));
		String clusterSubnet = json.get("cluster_subnet").textValue();
		String serviceSubnet = json.get("service_subnet").textValue();
		Vpc.Id vpc = Vpc.id(json.get("vpc_uuid").textValue());
		String ipv4;
		JsonNode ipv4Node = json.get("ipv4");
		if (ipv4Node == null)
			ipv4 = "";
		else
			ipv4 = ipv4Node.textValue();
		String endpoint = json.get("endpoint").textValue();

		Set<String> tags = new LinkedHashSet<>();
		JsonNode tagsNode = json.get("tags");
		for (JsonNode tag : tagsNode)
			tags.add(tag.textValue());

		JsonNode nodePoolsNode = json.get("node_pools");
		Set<NodePool> nodePools = new HashSet<>();
		for (JsonNode node : nodePoolsNode)
			nodePools.add(nodePoolFromServer(client, node));

		MaintenanceSchedule maintenanceSchedule = maintenanceScheduleFromServer(
			json.get("maintenance_policy"));
		boolean autoUpgrade = getBoolean(json, "auto_upgrade");
		Status status = statusFromServer(json.get("status"));

		Instant createdAt = Instant.parse(json.get("created_at").textValue());
		Instant updatedAt = Instant.parse(json.get("updated_at").textValue());
		boolean surgeUpgrade = getBoolean(json, "surge_upgrade");
		boolean ha = getBoolean(json, "ha");
		boolean canAccessRegistry = getBoolean(json, "registry_enabled");
		return new DefaultKubernetes(client, id, name, region, version, clusterSubnet, serviceSubnet, vpc, ipv4,
			endpoint, tags, nodePools, maintenanceSchedule, autoUpgrade, status, surgeUpgrade, ha,
			canAccessRegistry, createdAt, updatedAt);
	}

	public Kubernetes kubernetesToServer(DefaultKubernetesClient client, JsonNode kubernetesCluster)
	{
		return null;
	}

	/**
	 * Converts a Kubernetes.NodePool from its server representation.
	 *
	 * @param client the client configuration
	 * @param json   the JSON representation
	 * @return the node pool
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	private Kubernetes.NodePool nodePoolFromServer(KubernetesClient client, JsonNode json)
	{
		DropletType.Id dropletType = DropletType.id(json.get("size").textValue());
		String id = json.get("id").textValue();
		String name = json.get("name").textValue();
		int initialNumberOfNodes = getInt(json, "count");

		Set<String> tags = new LinkedHashSet<>();
		JsonNode tagsNode = json.get("tags");
		for (JsonNode tag : tagsNode)
			tags.add(tag.textValue());

		Set<String> labels = new LinkedHashSet<>();
		JsonNode labelsNode = json.get("labels");
		for (JsonNode label : labelsNode)
			labels.add(label.textValue());

		Set<String> taints = new LinkedHashSet<>();
		JsonNode taintsNode = json.get("taints");
		for (JsonNode taint : taintsNode)
			taints.add(taint.textValue());

		boolean autoScale = getBoolean(json, "auto_scale");
		int minNodes;
		int maxNodes;
		if (autoScale)
		{
			minNodes = getInt(json, "min_nodes");
			maxNodes = getInt(json, "max_nodes");
		}
		else
		{
			minNodes = initialNumberOfNodes;
			maxNodes = initialNumberOfNodes;
		}

		Set<Node> nodes = new HashSet<>();
		JsonNode nodesNode = json.get("nodes");
		for (JsonNode node : nodesNode)
			nodes.add(nodeFromServer(node));
		return new NodePool(id, name, dropletType, initialNumberOfNodes, tags, labels, taints, autoScale,
			minNodes, maxNodes, nodes);
	}

	/**
	 * Converts a KubernetesCreator.NodePool to its server representation.
	 *
	 * @param client the client configuration
	 * @param value  the node pool
	 * @return the server representation
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	public JsonNode nodePoolToServer(DefaultKubernetesClient client, NodePoolBuilder value)
	{
		ObjectNode json = client.getJsonMapper().createObjectNode().
			put("size", value.dropletType().getValue()).
			put("name", value.name()).
			put("count", value.initialNumberOfNodes());
		if (!value.tags().isEmpty())
		{
			ArrayNode array = json.putArray("tags");
			for (String tag : value.tags())
				array.add(tag);
		}
		if (!value.labels().isEmpty())
		{
			ArrayNode array = json.putArray("labels");
			for (String label : value.labels())
				array.add(label);
		}
		if (!value.taints().isEmpty())
		{
			ArrayNode array = json.putArray("taints");
			for (String taint : value.taints())
				array.add(taint);
		}
		if (value.autoScale())
		{
			json.put("auto_scale", true);
			json.put("min_nodes", value.minNodes());
			json.put("max_nodes", value.maxNodes());
		}
		return json;
	}

	/**
	 * Converts a Kubernetes.MaintenanceSchedule from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the maintenance schedule
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	private MaintenanceSchedule maintenanceScheduleFromServer(JsonNode json)
	{
		OffsetTime startTime = LocalTime.parse(json.get("start_time").textValue(), Strings.HOUR_MINUTE).
			atOffset(ZoneOffset.UTC);
		String dayAsString = json.get("day").textValue();
		DayOfWeek day;
		if (dayAsString.equals("any"))
			day = null;
		else
			day = DayOfWeek.valueOf(dayAsString.toUpperCase(Locale.ROOT));
		return new MaintenanceSchedule(startTime, day);
	}

	/**
	 * Converts a Kubernetes.Status from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the status
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	private Status statusFromServer(JsonNode json)
	{
		State state = stateFromServer(json.get("state"));
		JsonNode messageNode = json.get("message");
		String message;
		if (messageNode == null)
			message = "";
		else
			message = messageNode.textValue();
		return new Status(state, message);
	}

	/**
	 * Converts a Kubernetes.State from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the state
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	private State stateFromServer(JsonNode json)
	{
		return DefaultKubernetes.State.valueOf(json.textValue().toUpperCase(Locale.ROOT));
	}

	/**
	 * Converts Kubernetes.MaintenanceSchedule to its server representation.
	 *
	 * @param client the client configuration
	 * @param value  the maintenance schedule
	 * @return the server representation
	 * @throws IllegalStateException if the client is closed
	 */
	public JsonNode maintenanceScheduleToServer(DefaultKubernetesClient client, MaintenanceSchedule value)
	{
		ObjectNode json = client.getJsonMapper().createObjectNode();
		OffsetTime startTimeAtUtc = value.startTime().withOffsetSameInstant(ZoneOffset.UTC);
		json.put("start_time", Strings.HOUR_MINUTE.format(startTimeAtUtc));
		if (value.day() == null)
			json.put("day", "any");
		else
			json.put("day", value.day().name().toLowerCase(Locale.ROOT));
		return json;
	}

	/**
	 * Converts Kubernetes.Node from its server representation.
	 *
	 * @return the node
	 * @throws IllegalStateException if the client is closed
	 */
	private Node nodeFromServer(JsonNode json)
	{
		String id = json.get("id").textValue();
		String name = json.get("name").textValue();
		NodeStatus status = NodeStatusFromServer(json.get("status"));
		String dropletId = json.get("droplet_id").textValue();
		Instant createdAt = Instant.parse(json.get("created_at").textValue());
		Instant updatedAt = Instant.parse(json.get("updated_at").textValue());
		return new Node(id, name, status, dropletId, createdAt, updatedAt);
	}

	/**
	 * Converts Kubernetes.NodeStatus from its server representation.
	 *
	 * @return the node status
	 * @throws IllegalStateException if the client is closed
	 */
	private NodeStatus NodeStatusFromServer(JsonNode json)
	{
		NodeState state = nodeStateFromServer(json.get("state"));
		JsonNode messageNode = json.get("message");
		String message;
		if (messageNode == null)
			message = "";
		else
			message = messageNode.textValue();
		return new NodeStatus(state, message);
	}

	/**
	 * Converts Kubernetes.NodeState from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the node state
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the server response could not be parsed
	 */
	private NodeState nodeStateFromServer(JsonNode json)
	{
		return NodeState.valueOf(json.textValue().toUpperCase(Locale.ROOT));
	}
}