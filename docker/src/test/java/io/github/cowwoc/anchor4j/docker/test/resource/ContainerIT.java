package io.github.cowwoc.anchor4j.docker.test.resource;

import io.github.cowwoc.anchor4j.core.internal.client.Processes;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.docker.resource.Container;
import io.github.cowwoc.anchor4j.docker.resource.ContainerElement;
import io.github.cowwoc.anchor4j.docker.resource.ContainerLogs.LogStreams;
import io.github.cowwoc.anchor4j.docker.resource.ContainerState;
import io.github.cowwoc.anchor4j.docker.resource.ContainerState.Status;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.test.IntegrationTestContainer;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

import static io.github.cowwoc.anchor4j.docker.test.resource.ImageIT.EXISTING_IMAGE;
import static io.github.cowwoc.anchor4j.docker.test.resource.ImageIT.MISSING_IMAGE;
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
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		Container container = image.createContainer().create();
		ContainerState containerState = container.getState();
		Status status = containerState.getStatus();
		requireThat(status, "status").isEqualTo(Status.CREATED);
		it.onSuccess();
	}

	@Test
	public void createMultipleAnonymous() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		Container container1 = image.createContainer().create();
		ContainerState containerState1 = container1.getState();
		Container container2 = image.createContainer().create();
		ContainerState containerState2 = container2.getState();
		requireThat(containerState1, "container1").isNotEqualTo(containerState2, "container2");
		it.onSuccess();
	}

	@Test(expectedExceptions = ResourceInUseException.class)
	public void createWithConflictingName() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		image.createContainer().name(it.getName()).create();
		try
		{
			image.createContainer().name(it.getName()).create();
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
		Docker client = it.getClient();
		try
		{
			client.createContainer(MISSING_IMAGE).create();
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
		Docker client = it.getClient();
		List<ContainerElement> containers = client.listContainers();
		requireThat(containers, "containers").isEmpty();
		it.onSuccess();
	}

	@Test
	public void list() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		Container container = image.createContainer().create();
		ContainerState containerState = container.getState();
		List<ContainerElement> containers = client.listContainers();
		requireThat(containers, "containers").size().isEqualTo(1);
		ContainerElement element = containers.getFirst();
		requireThat(element.container().getId(), "element.container.getId()").
			isEqualTo(containerState.getId(), "container.getId()");
		requireThat(element.name(), "element.name()").isEqualTo(containerState.getName(), "container.getName()");
		it.onSuccess();
	}

	@Test
	public void get() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		Container container = image.createContainer().create();
		ContainerState containerState1 = container.getState();
		ContainerState containerState2 = client.getContainerState(containerState1.getId());
		requireThat(containerState1, "container1").isEqualTo(containerState2, "container2");
		it.onSuccess();
	}

	@Test
	public void getMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		ContainerState container = client.getContainerState(MISSING_CONTAINER);
		requireThat(container, "container").isNull();
		it.onSuccess();
	}

	@Test
	public void start() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		Container container = image.createContainer().arguments(KEEP_ALIVE).create();
		container.starter().start();
		ContainerState containerState = container.getState();
		Status status = containerState.getStatus();
		requireThat(status, "status").isEqualTo(Status.RUNNING);
		it.onSuccess();
	}

	@Test
	public void alreadyStarted() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		Container container = image.createContainer().arguments(KEEP_ALIVE).create();
		container.starter().start();
		container.starter().start();
		ContainerState containerState = container.getState();
		Status status = containerState.getStatus();
		requireThat(status, "status").isEqualTo(Status.RUNNING);
		it.onSuccess();
	}

	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void startMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		Container container = image.createContainer().create();
		container.remover().remove();
		try
		{
			container.starter().start();
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
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		Container container = image.createContainer().arguments(KEEP_ALIVE).create();
		container.starter().start();
		container.stopper().stop();
		ContainerState containerState = container.getState();
		Status status = containerState.getStatus();
		requireThat(status, "status").isEqualTo(Status.EXITED);
		it.onSuccess();
	}

	@Test
	public void alreadyStopped() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		Container container = image.createContainer().arguments(KEEP_ALIVE).create();
		container.starter().start();
		container.stopper().stop();
		ContainerState containerState = container.getState();
		Status status = containerState.getStatus();
		requireThat(status, "status").isEqualTo(Status.EXITED);
		it.onSuccess();
	}

	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void stopMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		Container container = image.createContainer().create();
		container.starter().start();
		container.remover().kill().removeAnonymousVolumes().remove();
		try
		{
			container.stopper().stop();
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
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();

		int expected = 3;
		Container container = image.createContainer().arguments("sh", "-c", "sleep 3; exit " + expected).
			create();
		container.starter().start();

		// Make sure we begin waiting before the container has shut down
		ContainerState containerState = container.getState();
		requireThat(containerState.getStatus(), "container.getStatus()").isNotEqualTo(Status.EXITED);
		int actual = client.waitUntilContainerStops(containerState.getId());
		requireThat(actual, "actual").isEqualTo(expected, "expected");
		it.onSuccess();
	}

	@Test
	public void waitUntilAlreadyStopped() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		int expected = 1;
		Container container = image.createContainer().arguments("sh", "-c", "exit " + expected).create();
		container.starter().start();
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
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		int expected = 1;
		Container container = image.createContainer().arguments("sh", "-c", "exit " + expected).create();
		container.starter().start();
		container.stopper().stop();
		container.remover().removeAnonymousVolumes().remove();
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

	@Test
	public void getContainerLogs() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		List<String> command = List.of("sh", "-c", "echo This is stdout; echo This is stderr >&2; exit 123");
		Container container = image.createContainer().arguments(command).create();
		container.starter().start();
		LogStreams containerLogs = container.getLogs().follow().stream();

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