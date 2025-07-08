package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Context;
import io.github.cowwoc.anchor4j.docker.resource.ContextCreator;
import io.github.cowwoc.anchor4j.docker.resource.ContextEndpoint;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Default implementation of {@code ContextCreator}.
 */
public final class DefaultContextCreator implements ContextCreator
{
	private final InternalDockerClient client;
	private final String name;
	private final ContextEndpoint endpoint;
	private String description = "";

	/**
	 * Creates a context creator.
	 *
	 * @param client   the client configuration
	 * @param name     the name of the context
	 * @param endpoint the connection configuration for the target Docker Engine
	 * @throws NullPointerException     if {@code name} or {@code endpoint} are null
	 * @throws IllegalArgumentException if {@code name} contains whitespace or is empty
	 * @see ContextEndpoint#builder(URI)
	 */
	public DefaultContextCreator(InternalDockerClient client, String name, ContextEndpoint endpoint)
	{
		assert client != null;
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();
		requireThat(endpoint, "endpoint").isNotNull();
		this.client = client;
		this.name = name;
		this.endpoint = endpoint;
	}

	@Override
	public ContextCreator description(String description)
	{
		requireThat(description, "description").doesNotContainWhitespace();
		this.description = description;
		return this;
	}

	@Override
	public Context apply() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/context/create/
		List<String> arguments = new ArrayList<>(11);
		arguments.add("context");
		arguments.add("create");
		if (!description.isEmpty())
		{
			arguments.add("--description");
			arguments.add(description);
		}

		StringJoiner endpointJoiner = new StringJoiner(",");
		endpointJoiner.add("host=" + endpoint.uri());
		if (endpoint.caPublicKey() != null)
		{
			endpointJoiner.add("ca=" + endpoint.caPublicKey());
			endpointJoiner.add("cert=" + endpoint.clientCertificate());
			endpointJoiner.add("key=" + endpoint.clientPrivateKey());
		}
		arguments.add("--docker");
		arguments.add(endpointJoiner.toString());
		arguments.add(name);

		CommandResult result = client.retry(_ -> client.run(arguments));
		client.getContextParser().create(result);
		return client.getContext(Context.id(name));
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultContextCreator.class).
			add("description", description).
			toString();
	}
}