package io.github.cowwoc.anchor4j.container.docker.test.resource;

import io.github.cowwoc.anchor4j.container.docker.test.IntegrationTestContainer;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Context;
import io.github.cowwoc.anchor4j.docker.resource.Context.Id;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class ContextIT
{
	@Test
	public void getClientContext() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Id clientContext = client.getClientContext();
		requireThat(clientContext.getValue(), "clientContext").isEqualTo(it.getName());
		it.onSuccess();
	}

	@Test
	public void listContexts() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		List<Context> contexts = client.getContexts();
		boolean matchFound = false;
		for (Context context : contexts)
		{
			if (context.getId().getValue().equals(it.getName()))
			{
				matchFound = true;
				break;
			}
		}
		requireThat(matchFound, "matchFound").withContext(contexts, "contexts").isTrue();
		it.onSuccess();
	}
}