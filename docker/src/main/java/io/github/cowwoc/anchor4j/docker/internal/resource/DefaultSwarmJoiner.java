package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDocker;
import io.github.cowwoc.anchor4j.docker.resource.JoinToken;
import io.github.cowwoc.anchor4j.docker.resource.SwarmJoiner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * The default implementation of {@code SwarmJoiner}.
 */
public final class DefaultSwarmJoiner implements SwarmJoiner
{
	private final InternalDocker client;
	private String advertiseAddress = "";
	private InetAddress dataPathAddress;
	private InetSocketAddress listenAddress;

	/**
	 * Creates a swarm joiner.
	 *
	 * @param client the client configuration
	 */
	public DefaultSwarmJoiner(InternalDocker client)
	{
		assert client != null;
		this.client = client;
	}

	@Override
	public SwarmJoiner advertiseAddress(InetSocketAddress advertiseAddress)
	{
		requireThat(advertiseAddress, "advertiseAddress").isNotNull();
		int port = advertiseAddress.getPort();
		if (port == 0)
			port = 2377;
		this.advertiseAddress = advertiseAddress.getHostString() + ":" + port;
		return this;
	}

	@Override
	public SwarmJoiner advertiseAddress(String advertiseAddress)
	{
		requireThat(advertiseAddress, "advertiseAddress").doesNotContainWhitespace().isNotEmpty();
		this.advertiseAddress = advertiseAddress;
		return this;
	}

	@Override
	public SwarmJoiner dataPathAddress(InetAddress dataPathAddress)
	{
		requireThat(dataPathAddress, "dataPathAddress").isNotNull();
		this.dataPathAddress = dataPathAddress;
		return this;
	}

	@Override
	public SwarmJoiner listenAddress(InetSocketAddress listenAddress)
	{
		requireThat(listenAddress, "listenAddress").isNotNull();
		this.listenAddress = listenAddress;
		return this;
	}

	@Override
	public void join(JoinToken joinToken) throws IOException, InterruptedException
	{
		requireThat(joinToken, "joinToken").isNotNull();

		// https://docs.docker.com/reference/cli/docker/swarm/join/
		List<String> arguments = new ArrayList<>(11);
		arguments.add("swarm");
		arguments.add("join");
		if (!advertiseAddress.isEmpty())
		{
			arguments.add("--advertise-addr");
			arguments.add(advertiseAddress);
		}
		if (dataPathAddress != null)
		{
			arguments.add("--data-path-addr");
			arguments.add(dataPathAddress.getHostAddress());
		}
		if (listenAddress != null)
		{
			arguments.add("--listen-addr");
			arguments.add(listenAddress.getHostString() + ":" + listenAddress.getPort());
		}
		arguments.add("--token");
		arguments.add(joinToken.token());
		InetSocketAddress managerAddress = joinToken.managerAddress();
		arguments.add(managerAddress.getHostString() + ":" + managerAddress.getPort());
		CommandResult result = client.retry(deadline -> client.run(arguments, deadline));
		client.getSwarmParser().join(result);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultSwarmJoiner.class).
			add("advertiseAddress", advertiseAddress).
			add("dataPathAddress", dataPathAddress).
			add("listenAddress", listenAddress).
			toString();
	}
}