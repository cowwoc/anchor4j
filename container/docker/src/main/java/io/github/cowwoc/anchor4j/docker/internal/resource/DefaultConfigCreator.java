package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Config;
import io.github.cowwoc.anchor4j.docker.resource.Config.Id;
import io.github.cowwoc.anchor4j.docker.resource.ConfigCreator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class DefaultConfigCreator implements ConfigCreator
{
	/**
	 * The format of config names.
	 */
	public static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9-_.]{1,64}");
	private final InternalDockerClient client;
	private final Map<String, String> labels = new HashMap<>();

	/**
	 * Creates a config creator.
	 *
	 * @param client the client configuration
	 */
	public DefaultConfigCreator(InternalDockerClient client)
	{
		assert client != null;
		this.client = client;
	}

	@Override
	public ConfigCreator label(String name, String value)
	{
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();
		requireThat(value, "value").doesNotContainWhitespace().isNotEmpty();
		labels.put(name, value);
		return this;
	}

	@Override
	public Config apply(String name, String value) throws IOException, InterruptedException
	{
		return apply(name, ByteBuffer.wrap(value.getBytes(UTF_8)));
	}

	@Override
	public Config apply(String name, ByteBuffer value) throws IOException, InterruptedException
	{
		requireThat(name, "name").doesNotContainWhitespace().matches(NAME_PATTERN);
		requireThat(value.remaining(), "value.remaining()").isLessThanOrEqualTo(MAX_SIZE_IN_BYTES);

		// https://docs.docker.com/reference/cli/docker/config/create/
		List<String> arguments = new ArrayList<>(4 + labels.size() * 2);
		arguments.add("config");
		arguments.add("create");
		for (Entry<String, String> entry : labels.entrySet())
		{
			arguments.add("--label");
			arguments.add(entry.getKey() + "=" + entry.getValue());
		}
		arguments.add(name);
		arguments.add("-");
		CommandResult result = client.retry(_ -> client.run(arguments, value));
		Id id = Config.id(client.getConfigParser().create(result));
		return client.getConfig(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultConfigCreator.class).
			add("labels", labels).
			toString();
	}
}