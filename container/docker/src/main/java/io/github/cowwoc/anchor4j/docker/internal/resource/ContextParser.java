package io.github.cowwoc.anchor4j.docker.internal.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.cowwoc.anchor4j.container.core.internal.resource.AbstractContainerParser;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Context;
import io.github.cowwoc.anchor4j.docker.resource.Context.Id;
import io.github.cowwoc.anchor4j.docker.resource.ContextElement;
import io.github.cowwoc.anchor4j.docker.resource.ContextEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses responses to {@code Context} commands.
 */
public class ContextParser extends AbstractContainerParser
{
	private static final Pattern CONTEXT_NOT_FOUND = Pattern.compile("""
		context "([^"]+)" does not exist""");
	private static final Pattern CONFLICTING_NAME = Pattern.compile("""
		context "([^"]+)" already exists""");
	private static final Pattern REMOVE_FAILED_RESOURCE_IN_USE = Pattern.compile("""
		failed to remove context (.+?): failed to remove metadata: remove (.+?): The process cannot access the \
		file because it is being used by another process\\.""");
	private static final Pattern TLS_CERTIFICATE_NOT_FOUND = Pattern.compile("""
		unable to create docker endpoint config: open ([^:]+): The system cannot find the (?:file|path) \
		specified\\.""");
	private static final Pattern TLS_CERTIFICATE_MISSING_DATA = Pattern.compile("""
		unable to create docker endpoint config: invalid docker endpoint options: failed to retrieve context \
		tls info: tls: failed to find any PEM data in certificate input""");
	private final Logger log = LoggerFactory.getLogger(ContextParser.class);

	/**
	 * Creates a parser.
	 *
	 * @param client the client configuration
	 */
	public ContextParser(InternalDockerClient client)
	{
		super(client);
	}

	@Override
	protected InternalDockerClient getClient()
	{
		return (InternalDockerClient) super.getClient();
	}

	/**
	 * Lists all the contexts.
	 *
	 * @param result the result of executing a command
	 * @return the contexts
	 */
	public List<ContextElement> list(CommandResult result)
	{
		if (result.exitCode() != 0)
			throw result.unexpectedResponse();
		JsonMapper jm = getClient().getJsonMapper();
		try
		{
			String[] lines = SPLIT_LINES.split(result.stdout());
			List<ContextElement> elements = new ArrayList<>(lines.length);
			for (String line : lines)
			{
				if (line.isBlank())
					continue;
				JsonNode json = jm.readTree(line);
				boolean current = json.get("Current").booleanValue();
				String description = json.get("Description").textValue();
				String endpoint = json.get("DockerEndpoint").textValue();
				String error = json.get("Error").textValue();
				String name = json.get("Name").textValue();
				Id id;
				if (name.isEmpty())
					id = null;
				else
					id = Context.id(name);

				try
				{
					elements.add(new ContextElement(id, current, description, endpoint, error));
				}
				catch (IllegalArgumentException e)
				{
					log.error(json.toString(), e);
					throw e;
				}
			}
			return elements;
		}
		catch (JsonProcessingException e)
		{
			throw new AssertionError(e);
		}
	}

	/**
	 * Looks up a context.
	 *
	 * @param result the result of executing a command
	 * @return null if no match is found
	 */
	public Context getContext(CommandResult result)
	{
		if (result.exitCode() != 0)
			throw result.unexpectedResponse();
		JsonMapper jm = getClient().getJsonMapper();
		try
		{
			JsonNode json = jm.readTree(result.stdout());
			assert json.size() == 1 : json;
			JsonNode context = json.get(0);

			Context.Id actualId = Context.id(context.get("Name").textValue());
			JsonNode metadata = context.get("Metadata");
			JsonNode descriptionNode = metadata.get("Description");
			String description;
			if (descriptionNode == null)
				description = "";
			else
				description = descriptionNode.textValue();
			JsonNode endpoints = context.get("Endpoints");
			JsonNode dockerEndpoint = endpoints.get("docker");
			String endpoint = dockerEndpoint.get("Host").textValue();
			return new DefaultContext(getClient(), actualId, description, endpoint);
		}
		catch (JsonProcessingException e)
		{
			throw new AssertionError(e);
		}
	}

	/**
	 * Returns the current context's ID.
	 *
	 * @param result the result of executing a command
	 * @return null if the default context is used
	 */
	public Id show(CommandResult result)
	{
		if (result.exitCode() != 0)
			throw result.unexpectedResponse();
		String stdout = result.stdout();
		if (stdout.isEmpty())
			return null;
		return Context.id(stdout);
	}

	/**
	 * Set a client's current context.
	 *
	 * @param result the result of executing a command
	 */
	public void use(CommandResult result)
	{
		if (result.exitCode() != 0)
			throw result.unexpectedResponse();
	}

	/**
	 * Creates a context.
	 *
	 * @param result the result of executing a command
	 * @throws ResourceNotFoundException if any of the {@link ContextEndpoint referenced TLS files} is not
	 *                                   found
	 * @throws ResourceInUseException    if another context with the same name already exists
	 */
	public void create(CommandResult result) throws ResourceNotFoundException, ResourceInUseException
	{
		if (result.exitCode() != 0)
		{
			String stderr = result.stderr();
			Matcher matcher = CONFLICTING_NAME.matcher(stderr);
			if (matcher.matches())
				throw new ResourceInUseException("Name already in use: " + matcher.group(1));
			matcher = TLS_CERTIFICATE_NOT_FOUND.matcher(stderr);
			if (matcher.matches())
				throw new ResourceNotFoundException("TLS certificate not found: " + matcher.group(1));
			matcher = TLS_CERTIFICATE_MISSING_DATA.matcher(stderr);
			if (matcher.matches())
			{
				throw new ResourceNotFoundException("One of the TLS files referenced by the Context endpoint is " +
					"empty");
			}
			throw result.unexpectedResponse();
		}
	}

	/**
	 * Removes the context. If the context does not exist, this method has no effect.
	 *
	 * @param result the result of executing a command
	 * @throws IOException if an I/O error occurs. These errors are typically transient, and retrying the
	 *                     request may resolve the issue.
	 */
	public void remove(CommandResult result) throws IOException
	{
		if (result.exitCode() != 0)
		{
			String stderr = result.stderr();
			Matcher matcher = CONTEXT_NOT_FOUND.matcher(stderr);
			if (matcher.matches())
				return;
			matcher = REMOVE_FAILED_RESOURCE_IN_USE.matcher(stderr);
			if (matcher.matches())
			{
				throw new IOException("Failed to remove metadata because the file is being used by " +
					"another process.\n" +
					"Container: " + matcher.group(1) + "\n" +
					"File     : " + matcher.group(2));
			}
			throw result.unexpectedResponse();
		}
	}
}