package io.github.cowwoc.anchor4j.container.docker.test.resource;

import io.github.cowwoc.anchor4j.container.docker.test.IntegrationTestContainer;
import io.github.cowwoc.anchor4j.core.internal.util.Processes;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.docker.resource.Container;
import io.github.cowwoc.anchor4j.docker.resource.Container.Status;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.ProcessListener;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import static io.github.cowwoc.anchor4j.container.docker.test.resource.ImageIT.EXISTING_IMAGE;
import static io.github.cowwoc.anchor4j.container.docker.test.resource.ImageIT.MISSING_IMAGE;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class ContainerIT
{
	/**
	 * We assume that this container name will never exist.
	 */
	private static final String MISSING_CONTAINER = "ContainerIT.missing-container";
	/**
	 * A command that prevents the container from exiting.
	 */
	private static final String[] KEEP_ALIVE = {"tail", "-f", "/dev/null"};

	@Test
	public void create() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		Container container = image.createContainer().apply();
		Status status = container.getStatus();
		requireThat(status, "status").isEqualTo(Status.CREATED);
		it.onSuccess();
	}

	@Test
	public void createMultipleAnonymous() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		Container container1 = image.createContainer().apply();
		Container container2 = image.createContainer().apply();
		requireThat(container1, "container1").isNotEqualTo(container2, "container2");
		it.onSuccess();
	}

	@Test(expectedExceptions = ResourceInUseException.class)
	public void createWithConflictingName() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		image.createContainer().name(it.getName()).apply();
		try
		{
			image.createContainer().name(it.getName()).apply();
		}
		catch (ResourceInUseException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void createMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		try
		{
			client.createContainer(MISSING_IMAGE).apply();
		}
		catch (ResourceNotFoundException e)
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
		List<Container> containers = client.getContainers();
		requireThat(containers, "containers").isEmpty();
		it.onSuccess();
	}

	@Test
	public void list() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		Container container1 = image.createContainer().apply();

		List<Container> containers = client.getContainers();
		requireThat(containers, "containers").size().isEqualTo(1);
		Container container2 = containers.getFirst();
		requireThat(container1, "container1").isEqualTo(container2, "container2");
		it.onSuccess();
	}

	@Test
	public void get() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		Container container1 = image.createContainer().apply();
		Container container2 = client.getContainer(container1.getId());
		requireThat(container1, "container1").isEqualTo(container2, "container2");
		it.onSuccess();
	}

	@Test
	public void getMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Container container = client.getContainer(MISSING_CONTAINER);
		requireThat(container, "container").isNull();
		it.onSuccess();
	}

	@Test
	public void start() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		Container container = image.createContainer().arguments(KEEP_ALIVE).apply();
		container = container.start().apply();
		Status status = container.getStatus();
		requireThat(status, "status").isEqualTo(Status.RUNNING);
		it.onSuccess();
	}

	@Test
	public void alreadyStarted() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		Container container = image.createContainer().arguments(KEEP_ALIVE).apply();
		container = container.start().apply();
		container = container.waitUntilStatus(Status.RUNNING);
		container = container.start().apply();
		Status status = container.reload().getStatus();
		requireThat(status, "status").isEqualTo(Status.RUNNING);
		it.onSuccess();
	}

	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void startMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		Container container = image.createContainer().apply();
		container.remove().apply();
		try
		{
			container.start().apply();
		}
		catch (ResourceNotFoundException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	@Test
	public void stop() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		Container container = image.createContainer().arguments(KEEP_ALIVE).apply();
		container = container.start().apply();
		container = container.stop().apply();
		container.waitUntilStop();
		Status status = container.getStatus();
		requireThat(status, "status").isEqualTo(Status.EXITED);
		it.onSuccess();
	}

	@Test
	public void alreadyStopped() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		Container container = image.createContainer().arguments(KEEP_ALIVE).apply();
		container = container.start().apply();
		container = container.stop().apply();
		container.waitUntilStop();
		Status status = container.getStatus();
		requireThat(status, "status").isEqualTo(Status.EXITED);
		it.onSuccess();
	}

	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void stopMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		Container container = image.createContainer().apply();
		container = container.start().apply();
		container.remove().kill().removeAnonymousVolumes().apply();
		try
		{
			container.stop().apply();
		}
		catch (ResourceNotFoundException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	@Test
	public void waitUntilStopped() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();

		int expected = 3;
		Container container = image.createContainer().arguments("sh", "-c", "sleep 3; exit " + expected).apply();
		container = container.start().apply();
		int actual = container.waitUntilStop();
		requireThat(actual, "actual").isEqualTo(expected, "expected");
		it.onSuccess();
	}

	@Test
	public void waitUntilAlreadyStopped() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		int expected = 1;
		Container container = image.createContainer().arguments("sh", "-c", "exit " + expected).apply();
		container = container.start().apply();
		int actual = container.waitUntilStop();
		requireThat(actual, "actual").isEqualTo(expected, "expected");
		actual = container.waitUntilStop();
		requireThat(actual, "actual").isEqualTo(expected, "expected");
		it.onSuccess();
	}

	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void waitUntilMissingContainerStopped() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		int expected = 1;
		Container container = image.createContainer().arguments("sh", "-c", "exit " + expected).apply();
		container = container.start().apply();
		container = container.stop().apply();
		container.remove().removeAnonymousVolumes().apply();
		try
		{
			container.waitUntilStop();
		}
		catch (ResourceNotFoundException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	/**
	 * Waits until an event has occurred.
	 */
	@Test
	public void waitUntilEvent() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();

		int expected = 3;
		Container container = image.createContainer().arguments("sh", "-c", "sleep 3; exit " + expected).apply();
		container = container.start().apply();
		requireThat(container.getStatus(), "container.getStatus()").isNotEqualTo(Status.EXITED);
		container = container.waitUntilStatus(Status.EXITED);
		requireThat(container.getStatus(), "container.getStatus()").isEqualTo(Status.EXITED);
		it.onSuccess();
	}

	/**
	 * Try waiting until an event has occurred when the container does not exist.
	 */
	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void waitUntilMissingContainerEvent() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		int expected = 1;
		Container container = image.createContainer().arguments("sh", "-c", "exit " + expected).apply();
		container = container.start().apply();
		container = container.stop().apply();
		container.remove().removeAnonymousVolumes().apply();
		try
		{
			container.waitUntilStatus(Status.EXITED);
		}
		catch (ResourceNotFoundException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	@Test
	public void getContainerLogs() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		List<String> command = List.of("sh", "-c", "echo This is stdout; echo This is stderr >&2; exit 123");
		Container container = image.createContainer().arguments(command).apply();
		container = container.start().apply();
		ProcessListener containerLogs = container.getLogs().follow().apply();

		BlockingQueue<Throwable> exceptions = new LinkedBlockingQueue<>();
		StringJoiner stdoutJoiner = new StringJoiner("\n");
		StringJoiner stderrJoiner = new StringJoiner("\n");
		try (BufferedReader stdoutReader = containerLogs.getOutputReader();
		     BufferedReader stderrReader = containerLogs.getErrorReader())
		{
			Thread stdoutThread = Thread.startVirtualThread(() ->
				Processes.consume(stdoutReader, exceptions, stdoutJoiner::add));
			Thread stderrThread = Thread.startVirtualThread(() ->
				Processes.consume(stderrReader, exceptions, stderrJoiner::add));

			// We have to invoke Thread.join() to ensure that all the data is read. Blocking on Process.waitFor()
			// does not guarantee this.
			stdoutThread.join();
			stderrThread.join();
			int exitCode = container.waitUntilStop();
			String stdout = stdoutJoiner.toString();
			String stderr = stderrJoiner.toString();
			Path workingDirectory = Path.of(System.getProperty("user.dir"));
			CommandResult result = new CommandResult(command, workingDirectory, stdout, stderr,
				exitCode);
			requireThat(stdout, "stdout").withContext(result, "result").isEqualTo("This is stdout");
			requireThat(stderr, "stderr").withContext(result, "result").isEqualTo("This is stderr");
			requireThat(exitCode, "exitCode").withContext(result, "result").isEqualTo(123);
		}
		it.onSuccess();
	}
}