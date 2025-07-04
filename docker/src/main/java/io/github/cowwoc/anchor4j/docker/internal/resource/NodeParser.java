package io.github.cowwoc.anchor4j.docker.internal.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.cowwoc.anchor4j.core.internal.resource.AbstractParser;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmMemberException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.NodeElement;
import io.github.cowwoc.anchor4j.docker.resource.NodeRemover;
import io.github.cowwoc.anchor4j.docker.resource.NodeState;
import io.github.cowwoc.anchor4j.docker.resource.NodeState.Availability;
import io.github.cowwoc.anchor4j.docker.resource.NodeState.Reachability;
import io.github.cowwoc.anchor4j.docker.resource.NodeState.Role;
import io.github.cowwoc.anchor4j.docker.resource.NodeState.Status;
import io.github.cowwoc.anchor4j.docker.resource.TaskState;
import io.github.cowwoc.anchor4j.docker.resource.TaskState.State;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Parses server responses to {@code Node} commands.
 */
public class NodeParser extends AbstractParser
{
	// Known variants:
	// Error response from daemon: This node is not a swarm manager. Use "docker swarm init" or "docker swarm join" to connect this node to swarm and try again.
	// Error response from daemon: This node is not a swarm manager. Worker nodes can't be used to view or modify cluster state. Please run this command on a manager node or promote the current node to a manager.
	static final String NOT_SWARM_MANAGER = "Error response from daemon: This node is not a swarm manager.";
	private static final Pattern UNIX_SOCKET_MISSING = Pattern.compile("""
		Error response from daemon: rpc error: code = Unavailable desc = connection error: desc = \
		"transport: Error while dialing: dial (unix .+?): connect: no such file or directory""");
	private static final String ACCESS_DENIED_TO_WORKER = """
		Error response from daemon: This node is not a swarm manager. Worker nodes can't be used to view or \
		modify cluster state. Please run this command on a manager node or promote the current node to a \
		manager.""";

	/**
	 * Creates a parser.
	 *
	 * @param client the client configuration
	 */
	public NodeParser(InternalDocker client)
	{
		super(client);
	}

	@Override
	protected InternalDocker getClient()
	{
		return (InternalDocker) super.getClient();
	}

	/**
	 * Lists all the nodes in the swarm.
	 *
	 * @param result the result of executing a command
	 * @return the nodes in the swarm
	 * @throws NullPointerException     if {@code filters} is null
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 */
	public List<NodeElement> listNodes(CommandResult result)
	{
		if (result.exitCode() != 0)
		{
			if (result.stderr().startsWith(NOT_SWARM_MANAGER))
				throw new NotSwarmManagerException();
			throw result.unexpectedResponse();
		}
		JsonMapper jm = getClient().getJsonMapper();
		try
		{
			String[] lines = SPLIT_LINES.split(result.stdout());
			List<NodeElement> elements = new ArrayList<>(lines.length);
			for (String line : lines)
			{
				if (line.isBlank())
					continue;
				JsonNode json = jm.readTree(line);
				Availability availability = getAvailability(json.get("Availability"));
				String engineVersion = json.get("EngineVersion").textValue();
				String hostname = json.get("Hostname").textValue();
				String id = json.get("ID").textValue();

				JsonNode managerStatusNode = json.get("ManagerStatus");
				Role role;
				boolean leader;
				Reachability reachability;
				if (managerStatusNode == null)
				{
					role = Role.WORKER;
					leader = false;
					reachability = Reachability.UNKNOWN;
				}
				else
				{
					role = Role.MANAGER;
					String status = managerStatusNode.textValue();
					switch (status)
					{
						case "Leader" ->
						{
							leader = true;
							reachability = Reachability.REACHABLE;
						}
						case "Reachable" ->
						{
							leader = false;
							reachability = Reachability.REACHABLE;
						}
						default -> throw new AssertionError("Unexpected value: " + status);
					}
				}
				Status status = getStatus(json.get("Status"));
				elements.add(new NodeElement(getClient().node(id), hostname, role, leader, status, reachability,
					availability, engineVersion));
			}
			return elements;
		}
		catch (JsonProcessingException e)
		{
			throw new AssertionError(e);
		}
	}

	/**
	 * @param json the JSON representation of the Availability
	 * @return the enum value
	 */
	private static Availability getAvailability(JsonNode json)
	{
		return Availability.valueOf(json.textValue().toUpperCase(Locale.ROOT));
	}

	/**
	 * @param json the JSON representation of a Status
	 * @return the enum value
	 */
	private static Status getStatus(JsonNode json)
	{
		return Status.valueOf(json.textValue().toUpperCase(Locale.ROOT));
	}

	/**
	 * Looks up a node by its ID or name.
	 *
	 * @param result the result of executing a command
	 * @return null if no match is found
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws FileNotFoundException    if the {@link Docker#getClientContext() referenced context} referenced a
	 *                                  unix socket that was not found
	 * @throws ConnectException         if the {@link Docker#getClientContext() referenced context} referenced a
	 *                                  TCP/IP socket that refused a connection
	 */
	public NodeState getState(CommandResult result) throws FileNotFoundException, ConnectException
	{
		if (result.exitCode() != 0)
		{
			if (result.stderr().startsWith(NOT_SWARM_MANAGER))
				throw new NotSwarmManagerException();
			Matcher matcher = UNIX_SOCKET_MISSING.matcher(result.stderr());
			if (matcher.matches())
				throw new FileNotFoundException("No such file or directory: " + matcher.group(1));
			throw result.unexpectedResponse();
		}
		JsonMapper jm = getClient().getJsonMapper();
		try
		{
			JsonNode json = jm.readTree(result.stdout());
			assert json.size() == 1 : json;
			JsonNode node = json.get(0);

			String id = node.get("ID").textValue();
			JsonNode spec = node.get("Spec");
			Availability availability = Availability.valueOf(spec.get("Availability").textValue().
				toUpperCase(Locale.ROOT));
			Role role = getType(spec.get("Role"));
			JsonNode labelsNode = spec.get("Labels");
			List<String> labels = new ArrayList<>(labelsNode.size());
			for (JsonNode label : labelsNode)
			{
				String keyValue = label.textValue();
				int separator = keyValue.indexOf('=');
				if (separator == -1)
					throw new IllegalArgumentException("Labels must follow the format: key=value.\n" +
						"Actual: " + keyValue);
				String key = keyValue.substring(0, separator);
				requireThat(key, "key").matches("^[a-zA-Z0-9.-_]+$");
				labels.add(keyValue);
			}
			// Reminder: spec.labels are used to constrain task scheduling (e.g., zone=us-east, role=worker) while
			// description.engine.labels are informational (e.g., operation-system, version)

			JsonNode description = node.get("Description");
			String hostname = description.get("Hostname").textValue();

			JsonNode engine = description.get("Engine");
			String engineVersion = engine.get("EngineVersion").textValue();

			JsonNode statusNode = node.get("Status");
			Status status = getStatus(statusNode.get("State"));
			String address = statusNode.get("Addr").textValue();

			JsonNode managerStatusNode = node.get("ManagerStatus");
			boolean leader;
			Reachability reachability;
			String managerAddress;
			if (managerStatusNode == null)
			{
				// Worker
				leader = false;
				reachability = Reachability.UNKNOWN;
				managerAddress = "";
			}
			else
			{
				leader = getBoolean(managerStatusNode, "Leader");
				reachability = getReachability(managerStatusNode.get("Reachability"));
				managerAddress = managerStatusNode.get("Addr").textValue();
			}
			return new NodeState(getClient(), id, hostname, role, leader, status, reachability, availability,
				managerAddress, address, labels, engineVersion);
		}
		catch (JsonProcessingException e)
		{
			throw new AssertionError(e);
		}
	}

	/**
	 * @param json the JSON representation of a type
	 * @return the enum value
	 */
	private static Role getType(JsonNode json)
	{
		return Role.valueOf(json.textValue().toUpperCase(Locale.ROOT));
	}

	/**
	 * @param json the JSON representation of the Reachability
	 * @return the enum value
	 */
	private static Reachability getReachability(JsonNode json)
	{
		return Reachability.valueOf(json.textValue().toUpperCase(Locale.ROOT));
	}

	/**
	 * Removes a node from the swarm. If the node does not exist, this method has no effect.
	 *
	 * @param result the result of executing a command
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws ResourceInUseException   if the node is inaccessible and {@link NodeRemover#force()} was not
	 *                                  used, or if an attempt was made to remove the current node
	 */
	public void remove(CommandResult result) throws ResourceInUseException
	{
		if (result.exitCode() != 0)
		{
			if (result.stderr().startsWith(NOT_SWARM_MANAGER))
				throw new NotSwarmManagerException();
			throw result.unexpectedResponse();
		}
	}

	/**
	 * Updates a node.
	 *
	 * @param result the result of executing a command
	 * @return the updated ID of the node
	 */
	public String setRole(CommandResult result)
	{
		if (result.exitCode() != 0)
			throw result.unexpectedResponse();
		return result.stdout();
	}

	/**
	 * Lists the tasks that are running on a node.
	 *
	 * @param result the result of executing a command
	 * @return the tasks
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 */
	public List<TaskState> listTasksByNode(CommandResult result)
	{
		if (result.exitCode() != 0)
		{
			if (result.stderr().equals(ACCESS_DENIED_TO_WORKER))
				throw new NotSwarmManagerException();
			throw result.unexpectedResponse();
		}

		JsonMapper jm = getClient().getJsonMapper();
		try
		{
			String[] lines = SPLIT_LINES.split(result.stdout());
			List<TaskState> tasks = new ArrayList<>(lines.length);
			for (String line : lines)
			{
				if (line.isBlank())
					continue;
				JsonNode json = jm.readTree(line);
				String id = json.get("ID").textValue();
				String name = json.get("Name").textValue();
				State state = getState(json.get("CurrentState"));
				tasks.add(new TaskState(getClient(), id, name, state));
			}
			return tasks;
		}
		catch (JsonProcessingException e)
		{
			throw new AssertionError(e);
		}
	}

	/**
	 * @param json the JSON representation of the status
	 * @return the enum value
	 */
	private State getState(JsonNode json)
	{
		String message = json.textValue();
		int delimiter = message.indexOf(' ');
		if (delimiter == -1)
			throw new AssertionError("Invalid state: " + message);
		String state = message.substring(0, delimiter);
		return State.valueOf(state.toUpperCase(Locale.ROOT));
	}

	/**
	 * Lists a service's tasks.
	 *
	 * @param result the result of executing a command
	 * @return the tasks
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 */
	public List<TaskState> listTasksByService(CommandResult result)
	{
		return listTasksByNode(result);
	}

	/**
	 * Looks up the current node's ID.
	 *
	 * @param result the result of executing a command
	 * @return the ID
	 * @throws NotSwarmMemberException if the node is not a member of a swarm
	 */
	public String getNodeId(CommandResult result)
	{
		if (result.exitCode() != 0)
			throw result.unexpectedResponse();

		JsonMapper jm = getClient().getJsonMapper();
		try
		{
			String id = jm.readTree(result.stdout()).textValue();
			if (id.isEmpty())
				throw new NotSwarmMemberException();
			return id;
		}
		catch (JsonProcessingException e)
		{
			throw new AssertionError(e);
		}
	}

	/**
	 * Looks up a task's state.
	 *
	 * @param result the result of executing a command
	 * @return the state
	 * @throws NotSwarmMemberException if the node is not a member of a swarm
	 */
	public TaskState getTaskState(CommandResult result)
	{
		if (result.exitCode() != 0)
			throw result.unexpectedResponse();

		JsonMapper jm = getClient().getJsonMapper();
		try
		{
			JsonNode json = jm.readTree(result.stdout());
			assert json.size() == 1 : json;
			JsonNode node = json.get(0);

			String id = node.get("ID").textValue();
			String name = node.get("Name").textValue();
			State state = getState(json.get("State"));
			return new TaskState(getClient(), id, name, state);
		}
		catch (JsonProcessingException e)
		{
			throw new AssertionError(e);
		}
	}
}