package io.github.cowwoc.anchor4j.container.docker.test.resource;

import io.github.cowwoc.anchor4j.container.docker.test.IntegrationTestContainer;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.resource.Config;
import io.github.cowwoc.anchor4j.docker.resource.SwarmCreator.WelcomePackage;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class ConfigIT
{
	@Test
	public void create() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		client.createSwarm().apply();

		String value = "key=value";
		Config config = client.createConfig().apply(it.getName(), value);
		requireThat(config.getValueAsString(), "config.getValueAsString").isEqualTo(value, "value");
		it.onSuccess();
	}

	@Test(expectedExceptions = NotSwarmManagerException.class)
	public void createNotSwarmManager() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		WelcomePackage welcomePackage = manager.getClient().createSwarm().apply();

		IntegrationTestContainer worker = new IntegrationTestContainer("worker");
		worker.getClient().joinSwarm().join(welcomePackage.workerJoinToken());
		try
		{
			worker.getClient().createConfig().apply(manager.getName(), "key=value");
		}
		catch (NotSwarmManagerException e)
		{
			manager.onSuccess();
			worker.onSuccess();
			throw e;
		}
	}

	@Test(expectedExceptions = ResourceInUseException.class)
	public void createExistingConfig() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		client.createSwarm().apply();

		client.createConfig().apply(it.getName(), "key=value");
		try
		{
			client.createConfig().apply(it.getName(), "key=value");
		}
		catch (ResourceInUseException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	@Test
	public void listEmpty() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		client.createSwarm().apply();

		List<Config> configs = client.getConfigs();
		requireThat(configs, "configs").isEmpty();
		it.onSuccess();
	}

	@Test
	public void list() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		client.createSwarm().apply();

		Config config = client.createConfig().apply(it.getName(), "key=value");
		List<Config> configs = client.getConfigs();
		requireThat(configs, "configs").isEqualTo(List.of(config));
		it.onSuccess();
	}

	@Test(expectedExceptions = NotSwarmManagerException.class)
	public void listNotSwarmManager() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		try
		{
			client.getConfigs();
		}
		catch (NotSwarmManagerException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	@Test
	public void get() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		client.createSwarm().apply();
		Config expected = client.createConfig().apply(it.getName(), "key=value");
		Config actual = expected.reload();
		requireThat(actual, "actual").isEqualTo(expected, "expected");
		it.onSuccess();
	}

	@Test(expectedExceptions = NotSwarmManagerException.class)
	public void getNotSwarmManager() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		client.createSwarm().apply();
		Config expected = client.createConfig().apply(it.getName(), "key=value");
		client.leaveSwarm().force().apply();

		try
		{
			Config _ = expected.reload();
		}
		catch (NotSwarmManagerException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	@Test
	public void getMissingConfig() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		client.createSwarm().apply();
		Config actual = client.getConfig("missing");
		requireThat(actual, "actual").isNull();
		it.onSuccess();
	}
}