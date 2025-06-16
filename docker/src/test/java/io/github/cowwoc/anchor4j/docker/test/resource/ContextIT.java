package io.github.cowwoc.anchor4j.docker.test.resource;

import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.anchor4j.docker.resource.ContextElement;
import io.github.cowwoc.anchor4j.docker.test.IntegrationTestContainer;
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
		Docker client = it.getClient();
		String clientContext = client.getClientContext();
		requireThat(clientContext, "clientContext").isEqualTo(it.getName());
		it.onSuccess();
	}

	@Test
	public void listContexts() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		List<ContextElement> contexts = client.listContexts();
		boolean matchFound = false;
		for (ContextElement element : contexts)
		{
			if (element.context().getName().equals(it.getName()))
			{
				matchFound = true;
				break;
			}
		}
		requireThat(matchFound, "matchFound").withContext(contexts, "contexts").isTrue();
		it.onSuccess();
	}
}