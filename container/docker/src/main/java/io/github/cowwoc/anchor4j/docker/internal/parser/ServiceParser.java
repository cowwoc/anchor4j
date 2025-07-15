package io.github.cowwoc.anchor4j.docker.internal.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.cowwoc.anchor4j.container.core.internal.parser.AbstractContainerParser;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultService;
import io.github.cowwoc.anchor4j.docker.resource.Service;
import io.github.cowwoc.anchor4j.docker.resource.ServiceElement;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.cowwoc.anchor4j.docker.internal.parser.NodeParser.NOT_SWARM_MANAGER;
import static io.github.cowwoc.anchor4j.docker.internal.parser.NodeParser.UNIX_SOCKET_MISSING;
import static java.util.regex.Pattern.DOTALL;

/**
 * Parses responses to {@code Task} commands.
 */
public class ServiceParser extends AbstractContainerParser
{
	private static final Pattern IMAGE_NOT_FOUND = Pattern.compile("^Unable to find image '([^']+)' locally.*",
		DOTALL);
	private static final Pattern CONFLICTING_NAME = Pattern.compile("""
		Error response from daemon: Conflict. The container name "([^"]+)" is already in use by container \
		"([^"]+)"\\. You have to remove \\(or rename\\) that container to be able to reuse that name\\.""");
	private static final String NOT_A_MANAGER = """
		Error response from daemon: This node is not a swarm manager. Worker nodes can't be used to view or \
		modify cluster state. Please run this command on a manager node or promote the current node to a \
		manager.""";

	/**
	 * Creates a parser.
	 *
	 * @param client the client configuration
	 */
	public ServiceParser(InternalDockerClient client)
	{
		super(client);
	}

	@Override
	protected InternalDockerClient getClient()
	{
		return (InternalDockerClient) super.getClient();
	}

	/**
	 * Creates a container.
	 *
	 * @param result the result of executing a command
	 * @return the ID of the new container
	 * @throws NotSwarmManagerException  if the current node is not a swarm manager
	 * @throws ResourceNotFoundException if the referenced image is not available locally and cannot be pulled
	 *                                   from Docker Hub, either because the repository does not exist or
	 *                                   requires different authentication credentials
	 * @throws ResourceInUseException    if the requested name is in use by another container
	 */
	public Service.Id create(CommandResult result) throws ResourceNotFoundException, ResourceInUseException
	{
		if (result.exitCode() != 0)
		{
			String stderr = result.stderr();
			Matcher matcher = IMAGE_NOT_FOUND.matcher(stderr);
			if (matcher.matches())
				throw new ResourceNotFoundException("Image not found: " + matcher.group(1));
			matcher = CONFLICTING_NAME.matcher(stderr);
			if (matcher.matches())
			{
				throw new ResourceInUseException("The container name \"" + matcher.group(1) + "\" is already in " +
					"use by container \"" + matcher.group(2) + "\". You have to remove (or rename) that container " +
					"to be able to reuse that name.");
			}
			if (stderr.equals(NOT_A_MANAGER))
				throw new NotSwarmManagerException();
			throw result.unexpectedResponse();
		}
		String stdout = result.stdout();
		Matcher matcher = SPLIT_LINES.matcher(stdout);
		if (!matcher.find())
			throw result.unexpectedResponse();
		String idAsString = stdout.substring(0, matcher.start());
		return Service.id(idAsString);
	}

	/**
	 * Looks up a service by its ID or name.
	 *
	 * @param result the result of executing a command
	 * @return null if no match is found
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws FileNotFoundException    if the {@link DockerClient#getClientContext() referenced context}
	 *                                  referenced a unix socket that was not found
	 * @throws ConnectException         if the {@link DockerClient#getClientContext() referenced context}
	 *                                  referenced a TCP/IP socket that refused a connection
	 */
	public Service getService(CommandResult result) throws FileNotFoundException
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
			JsonNode service = json.get(0);

			Service.Id id = serviceIdFromServer(service.get("ID"));
			JsonNode spec = service.get("Spec");
			String name = spec.get("Name").textValue();
			return new DefaultService(getClient(), id, name);
		}
		catch (JsonProcessingException e)
		{
			throw new AssertionError(e);
		}
	}

	/**
	 * Converts a Service.Id from its server representation.
	 *
	 * @param json the server representation
	 * @return the ID
	 */
	private Service.Id serviceIdFromServer(JsonNode json)
	{
		return Service.id(json.textValue());
	}

	/**
	 * Lists the services that are in a swarm.
	 *
	 * @param result the result of executing a command
	 * @return the tasks
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 */
	public List<ServiceElement> listServices(CommandResult result)
	{
		if (result.exitCode() != 0)
		{
			if (result.stderr().equals(NOT_SWARM_MANAGER))
				throw new NotSwarmManagerException();
			throw result.unexpectedResponse();
		}

		JsonMapper jm = getClient().getJsonMapper();
		try
		{
			String[] lines = SPLIT_LINES.split(result.stdout());
			List<ServiceElement> services = new ArrayList<>(lines.length);
			for (String line : lines)
			{
				if (line.isBlank())
					continue;
				JsonNode json = jm.readTree(line);
				Service.Id id = Service.id(json.get("ID").textValue());
				String name = json.get("Name").textValue();
				services.add(new ServiceElement(id, name));
			}
			return services;
		}
		catch (JsonProcessingException e)
		{
			throw new AssertionError(e);
		}
	}
}