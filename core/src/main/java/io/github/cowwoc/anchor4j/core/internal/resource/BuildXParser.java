package io.github.cowwoc.anchor4j.core.internal.resource;

import io.github.cowwoc.anchor4j.core.exception.ContextNotFoundException;
import io.github.cowwoc.anchor4j.core.internal.client.InternalClient;
import io.github.cowwoc.anchor4j.core.resource.BuilderState;
import io.github.cowwoc.anchor4j.core.resource.BuilderState.Status;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses server responses to {@code BuildService} commands.
 */
public final class BuildXParser extends AbstractParser
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

	/**
	 * Creates a parser.
	 *
	 * @param client the client configuration
	 */
	public BuildXParser(InternalClient client)
	{
		super(client);
	}

	/**
	 * Returns the platforms that images can be built for.
	 *
	 * @param result the result of executing a command
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
	 * Looks up a builder by its name.
	 *
	 * @param result the result of executing a command
	 * @return the builder, or {@code null} if no match is found
	 * @throws IOException if an I/O error occurs. These errors are typically transient, and retrying the
	 *                     request may resolve the issue.
	 */
	public BuilderState getState(CommandResult result) throws IOException
	{
		if (result.exitCode() != 0)
		{
			Matcher matcher = NOT_FOUND.matcher(result.stderr());
			if (matcher.matches())
				return null;
			throw result.unexpectedResponse();
		}
		String name = null;
		BuilderState.Status status = null;
		String error = "";
		for (String line : SPLIT_LINES.split(result.stdout()))
		{
			if (line.isBlank())
				continue;
			if (line.startsWith("Name:"))
				name = line.substring("Name:".length()).strip();
			if (line.startsWith("Status:"))
				status = getStatus(line.substring("Status:".length()).strip());
			if (line.startsWith("Error:"))
				error = line.substring("Error:".length()).strip();
		}
		if (status == null && !error.isEmpty())
			status = Status.ERROR;
		assert status != null : "stdout: " + result.stdout();
		return new BuilderState(getClient(), name, status, error);
	}

	/**
	 * @param value the command-line representation of the status
	 * @return the enum value
	 */
	private static Status getStatus(String value)
	{
		return Status.valueOf(value.toUpperCase(Locale.ROOT));
	}

	/**
	 * Creates a builder.
	 *
	 * @param result the result of executing a command
	 * @return the name of the builder
	 * @throws ContextNotFoundException if the Docker context cannot be found or resolved
	 */
	public String create(CommandResult result) throws ContextNotFoundException
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
		return result.stdout();
	}
}