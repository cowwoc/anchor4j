package io.github.cowwoc.anchor4j.docker.test.resource;

import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.resource.ConfigElement;
import io.github.cowwoc.anchor4j.docker.resource.ConfigState;
import io.github.cowwoc.anchor4j.docker.resource.SwarmCreator.WelcomePackage;
import io.github.cowwoc.anchor4j.docker.test.IntegrationTestContainer;
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
		Docker client = it.getClient();
		client.createSwarm().create();

		String value = "key=value";
		ConfigState config = client.createConfig().create(it.getName(), value).getState();
		requireThat(config.getValueAsString(), "config.getValueAsString").isEqualTo(value, "value");
		it.onSuccess();
	}

	@Test(expectedExceptions = NotSwarmManagerException.class)
	public void createNotSwarmManager() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		WelcomePackage welcomePackage = manager.getClient().createSwarm().create();

		IntegrationTestContainer worker = new IntegrationTestContainer("worker");
		worker.getClient().joinSwarm().join(welcomePackage.workerJoinToken());
		try
		{
			worker.getClient().createConfig().create(manager.getName(), "key=value");
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
		Docker client = it.getClient();
		client.createSwarm().create();

		client.createConfig().create(it.getName(), "key=value");
		try
		{
			client.createConfig().create(it.getName(), "key=value");
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
		Docker client = it.getClient();
		client.createSwarm().create();

		List<ConfigElement> configs = client.listConfigs();
		requireThat(configs, "configs").isEmpty();
		it.onSuccess();
	}

	@Test
	public void list() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		client.createSwarm().create();

		ConfigState config = client.createConfig().create(it.getName(), "key=value").getState();
		List<ConfigElement> configs = client.listConfigs();
		requireThat(configs, "configs").isEqualTo(List.of(new ConfigElement(config.getId(), config.getName())));
		it.onSuccess();
	}

	@Test(expectedExceptions = NotSwarmManagerException.class)
	public void listNotSwarmManager() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		try
		{
			client.listConfigs();
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
		Docker client = it.getClient();
		client.createSwarm().create();
		ConfigState expected = client.createConfig().create(it.getName(), "key=value").getState();
		ConfigState actual = expected.reload();
		requireThat(actual, "actual").isEqualTo(expected, "expected");
		it.onSuccess();
	}

	@Test(expectedExceptions = NotSwarmManagerException.class)
	public void getNotSwarmManager() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		client.createSwarm().create();
		ConfigState expected = client.createConfig().create(it.getName(), "key=value").getState();
		client.leaveSwarm().force().leave();

		try
		{
			ConfigState _ = expected.reload();
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
		Docker client = it.getClient();
		client.createSwarm().create();
		ConfigState actual = client.getConfigState("missing");
		requireThat(actual, "actual").isNull();
		it.onSuccess();
	}
}