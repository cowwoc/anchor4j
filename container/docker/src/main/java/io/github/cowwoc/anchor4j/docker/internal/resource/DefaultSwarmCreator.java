package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.SwarmCreator;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultSwarmCreator implements SwarmCreator
{
	public static final int DEFAULT_LISTEN_PORT = 2377;
	private final InternalDockerClient client;
	private String advertiseAddress = "";
	private InetSocketAddress dataPathAddress;
	private final Set<String> defaultAddressPool = new HashSet<>();
	private int subnetSize = 24;
	private InetSocketAddress listenAddress;

	/**
	 * Creates a swarm creator.
	 *
	 * @param client the client configuration
	 */
	public DefaultSwarmCreator(InternalDockerClient client)
	{
		assert client != null;
		this.client = client;
	}

	@Override
	public SwarmCreator advertiseAddress(InetSocketAddress advertiseAddress)
	{
		requireThat(advertiseAddress, "advertiseAddress").isNotNull();
		int port = advertiseAddress.getPort();
		if (port == 0)
			port = DEFAULT_LISTEN_PORT;
		this.advertiseAddress = advertiseAddress.getHostString() + ":" + port;
		return this;
	}

	@Override
	public SwarmCreator advertiseAddress(String advertiseAddress)
	{
		requireThat(advertiseAddress, "advertiseAddress").doesNotContainWhitespace().isNotEmpty();
		this.advertiseAddress = advertiseAddress;
		return this;
	}

	@Override
	public SwarmCreator dataPathAddress(InetSocketAddress dataPathAddress)
	{
		requireThat(dataPathAddress, "dataPathAddress").isNotNull();
		if (dataPathAddress.getPort() != 0)
			requireThat(dataPathAddress.getPort(), "dataPathAddress.getPort()").isBetween(1024, true, 49_151, true);
		this.dataPathAddress = dataPathAddress;
		return this;
	}

	@Override
	public SwarmCreator defaultAddressPool(String defaultAddressPool)
	{
		requireThat(defaultAddressPool, "defaultAddressPool").doesNotContainWhitespace().isNotEmpty();
		String[] components = defaultAddressPool.split("/");
		if (components.length != 2)
		{
			throw new IllegalArgumentException("defaultAddressPool must contain exactly one slash.\n" +
				"Actual: " + defaultAddressPool);
		}
		try
		{
			InetAddress address = InetAddress.ofLiteral(components[0]);
			int prefixLength = Integer.parseInt(components[1]);
			if (address instanceof Inet4Address)
			{
				requireThat(prefixLength, "prefixLength").withContext(defaultAddressPool, "defaultAddressPool").
					isBetween(0, true, 32, true);
			}
			assert address instanceof Inet6Address : address.getClass().getName();
			requireThat(prefixLength, "prefixLength").withContext(defaultAddressPool, "defaultAddressPool").
				isBetween(0, true, 128, true);
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("defaultAddressPool must be in CIDR notation.\n" +
				"Actual: " + defaultAddressPool, e);
		}
		this.defaultAddressPool.add(defaultAddressPool);
		return this;
	}

	@Override
	public SwarmCreator subnetSize(int subnetSize)
	{
		requireThat(subnetSize, "subnetSize").isBetween(16, true, 28, true);
		this.subnetSize = subnetSize;
		return this;
	}

	@Override
	public SwarmCreator listenAddress(InetSocketAddress listenAddress)
	{
		requireThat(listenAddress, "listenAddress").isNotNull();
		this.listenAddress = listenAddress;
		return this;
	}

	@Override
	public WelcomePackage apply() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/swarm/init/
		List<String> arguments = new ArrayList<>(8 + defaultAddressPool.size() * 2 + 4);
		arguments.add("swarm");
		arguments.add("init");
		if (!advertiseAddress.isEmpty())
		{
			arguments.add("--advertise-addr");
			arguments.add(advertiseAddress);
		}
		if (dataPathAddress != null)
		{
			arguments.add("--data-path-addr");
			arguments.add(dataPathAddress.getHostString());
			int port = dataPathAddress.getPort();
			if (port != 0)
			{
				arguments.add("--data-path-port");
				arguments.add(dataPathAddress.getHostString());
			}
		}
		for (String address : defaultAddressPool)
		{
			arguments.add("--default-addr-pool");
			arguments.add(address);
		}
		if (subnetSize != 24)
		{
			arguments.add("--default-addr-pool-mask-length");
			arguments.add(String.valueOf(subnetSize));
		}
		if (listenAddress != null)
		{
			arguments.add("--listen-addr");
			arguments.add(listenAddress.getHostString() + ":" + listenAddress.getPort());
		}
		CommandResult result = client.retry(_ -> client.run(arguments));
		return client.getSwarmParser().create(result);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultSwarmCreator.class).
			add("advertiseAddress", advertiseAddress).
			add("dataPathAddress", dataPathAddress).
			add("defaultAddressPool", defaultAddressPool).
			add("subnetSize", subnetSize).
			add("listenAddress", listenAddress).
			toString();
	}
}