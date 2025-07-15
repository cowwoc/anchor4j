package io.github.cowwoc.anchor4j.container.core.internal.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.cowwoc.anchor4j.container.core.exception.ContextNotFoundException;
import io.github.cowwoc.anchor4j.container.core.internal.client.InternalContainerClient;
import io.github.cowwoc.anchor4j.container.core.internal.resource.DefaultBuilder;
import io.github.cowwoc.anchor4j.container.core.internal.resource.DefaultBuilder.DefaultNode;
import io.github.cowwoc.anchor4j.container.core.resource.Builder;
import io.github.cowwoc.anchor4j.container.core.resource.Builder.Driver;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage.Id;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses responses to {@code BuildService} commands.
 */
public final class BuildXParser extends AbstractContainerParser
{
	// Known variants:
	// ERROR: failed to build: no builder ("[^"]+") found
	// ERROR: no builder "ImageIT.buildAndOutputDockerImageToTarFile" found
	public static final Pattern NOT_FOUND = Pattern.compile(
		"ERROR: (?:failed to build: )?no builder (\"[^\"]+\") found");
	// Example:
	// ERROR: failed to initialize builder wizardly_rosalind (wizardly_rosalind0): unable to parse docker host `ImageIT.buildAndOutputOciImageToDirectoryUsingDockerContainerDriver`
	//
	// Where:
	//   * wizardly_rosalind is the name of the builder.
	//   * wizardly_rosalind0 is the name of the builder node to be created. Each node wraps a Docker endpoint
	//     and other build-related metadata (e.g., platforms, driver config).
	//   * ImageIT.buildAndOutputOciImageToDirectoryUsingDockerContainerDriver is the name of the Docker context
	//     to create the builder on.
	private static final Pattern CONTEXT_NOT_FOUND = Pattern.compile(
		"ERROR: failed to initialize builder [^(]+ \\([^)]+\\): unable to parse docker host `([^`]+)`");
	// Example:
	// ERROR: failed to initialize builder lucid_torvalds (lucid_torvalds0): error during connect: Get "http://ImageIT.buildAndOutputOciImageToDirectoryUsingDockerContainerDriver:2375/v1.50/containers/buildx_buildkit_lucid_torvalds0/json": dial tcp: lookup ImageIT.buildAndOutputOciImageToDirectoryUsingDockerContainerDriver: no such host
	private static final Pattern NO_SUCH_HOST = Pattern.compile("""
		ERROR: failed to initialize builder [^(]+ \\([^)]+\\):(?:error during connect: )?Get "[^"]+: dial tcp: lookup ([^:]+): no such host""");
	private static final Pattern EXPORTING_TO_IMAGE = Pattern.compile("""
		#\\d+ exporting to image""");
	private static final Pattern EXPORTING_MANIFEST_LIST = Pattern.compile("""
		#\\d+ exporting manifest list ([^ ]+) [^ ]+ done""");

	/**
	 * Creates a BuildXParser.
	 *
	 * @param client the client configuration
	 */
	public BuildXParser(InternalContainerClient client)
	{
		super(client);
	}

	/**
	 * Returns the platforms that images can be built for.
	 *
	 * @param result the result of executing the command
	 * @return the platforms
	 */
	public Set<String> getSupportedBuildPlatforms(CommandResult result)
	{
		if (result.exitCode() != 0)
			throw result.unexpectedResponse();
		Set<String> platforms = new HashSet<>();
		for (String line : SPLIT_LINES.split(result.stdout()))
		{
			if (line.isBlank() || !line.startsWith("Platforms:"))
				continue;
			line = line.substring("Platforms:".length());
			for (String platform : line.split(","))
				platforms.add(platform.strip());
		}
		return platforms;
	}

	/**
	 * Lists all builders.
	 *
	 * @param result the result of executing the command
	 * @return an empty list if no match is found
	 * @throws IOException if an I/O error occurs. These errors are typically transient, and retrying the
	 *                     request may resolve the issue.
	 */
	public List<Builder> getBuilders(CommandResult result) throws IOException
	{
		if (result.exitCode() != 0)
		{
			Matcher matcher = NOT_FOUND.matcher(result.stderr());
			if (matcher.matches())
				return List.of();
			throw result.unexpectedResponse();
		}
		InternalContainerClient client = getClient();
		JsonMapper jm = client.getJsonMapper();
		List<Builder> builders = new ArrayList<>();
		for (String line : SPLIT_LINES.split(result.stdout()))
		{
			if (line.isBlank())
				continue;
			JsonNode json = jm.readTree(line);
			List<Builder.Node> nodes = new ArrayList<>();
			Builder.Id id = Builder.id(json.get("Name").textValue());
			Driver driver = driverFromServer(json.get("Driver").textValue());
			for (JsonNode node : json.get("Nodes"))
			{
				String name = node.get("Name").textValue();
				Builder.Node.Status status = getBuilderNodeStatus(node.get("Status").textValue());
				String error = node.get("Err").textValue();
				nodes.add(new DefaultNode(name, status, error));
			}
			builders.add(new DefaultBuilder(client, id, nodes, driver));
		}
		return builders;
	}

	/**
	 * Converts a Driver from a server representation.
	 *
	 * @param value the server representation
	 * @return the driver
	 */
	private Driver driverFromServer(String value)
	{
		return Driver.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'));
	}

	/**
	 * Converts a builder from its server representation.
	 *
	 * @param result the result of executing the command
	 * @return the builder
	 */
	public Builder builderFromServer(CommandResult result)
	{
		if (result.exitCode() != 0)
		{
			Matcher matcher = NOT_FOUND.matcher(result.stderr());
			if (matcher.matches())
				return null;
			throw result.unexpectedResponse();
		}
		Deque<String> lines = new ArrayDeque<>(Arrays.asList(SPLIT_LINES.split(result.stdout())));
		String name = null;
		String driverName = null;
		while (!lines.isEmpty())
		{
			String line = lines.remove();
			if (line.isBlank())
				continue;
			if (line.startsWith("Name:"))
				name = line.substring("Name:".length()).strip();
			if (line.startsWith("Driver:"))
				driverName = line.substring("Driver:".length()).strip();
			if (line.startsWith("Nodes:"))
				break;
		}
		assert name != null;
		assert driverName != null;
		Driver driver = driverFromServer(driverName);

		List<Builder.Node> nodes = new ArrayList<>();
		while (!lines.isEmpty())
			nodes.add(getNode(lines));
		return new DefaultBuilder(getClient(), Builder.id(name), nodes, driver);
	}

	/**
	 * Looks up a builder by its name.
	 *
	 * @param lines server response
	 * @return the builder, or {@code null} if no match is found
	 */
	private Builder.Node getNode(Deque<String> lines)
	{
		String name = null;
		Builder.Node.Status status = null;
		String error = "";
		while (!lines.isEmpty())
		{
			String line = lines.remove();
			if (line.isBlank())
				break;
			if (line.startsWith("Name:"))
				name = line.substring("Name:".length()).strip();
			if (line.startsWith("Status:"))
				status = getBuilderNodeStatus(line.substring("Status:".length()).strip());
			if (line.startsWith("Error:"))
				error = line.substring("Error:".length()).strip();
		}
		if (status == null && !error.isEmpty())
			status = Builder.Node.Status.ERROR;
		return new DefaultNode(name, status, error);
	}

	/**
	 * @param value the command-line representation of the status
	 * @return the enum value
	 */
	private static Builder.Node.Status getBuilderNodeStatus(String value)
	{
		return Builder.Node.Status.valueOf(value.toUpperCase(Locale.ROOT));
	}

	/**
	 * Creates a builder.
	 *
	 * @param result the result of executing the command
	 * @return the ID of the builder
	 * @throws ContextNotFoundException if the Docker context cannot be found or resolved
	 */
	public Builder.Id create(CommandResult result) throws ContextNotFoundException
	{
		if (result.exitCode() != 0)
		{
			String stderr = result.stderr();
			Matcher matcher = CONTEXT_NOT_FOUND.matcher(stderr);
			if (!matcher.matches())
				matcher = NO_SUCH_HOST.matcher(stderr);
			if (matcher.matches())
				throw new ContextNotFoundException(matcher.group(1));
			throw result.unexpectedResponse();
		}
		return Builder.id(result.stdout());
	}

	/**
	 * Parses the ID of the image exported by a build.
	 *
	 * @param result the result of executing the command
	 * @return the image ID
	 */
	public Id getImageIdFromBuildOutput(CommandResult result)
	{
		if (result.exitCode() != 0)
			throw result.unexpectedResponse();
		String[] split = SPLIT_LINES.split(result.stderr());
		ArrayDeque<String> lines = new ArrayDeque<>(split.length);
		Collections.addAll(lines, split);
		while (!lines.isEmpty())
		{
			String line = lines.remove();
			if (line.isBlank())
				continue;
			if (EXPORTING_TO_IMAGE.matcher(line).matches())
				break;
		}
		while (!lines.isEmpty())
		{
			String line = lines.remove();
			if (line.isBlank())
				continue;
			Matcher matcher = EXPORTING_MANIFEST_LIST.matcher(line);
			if (matcher.matches())
				return ContainerImage.id(matcher.group(1));
		}
		return null;
	}
}