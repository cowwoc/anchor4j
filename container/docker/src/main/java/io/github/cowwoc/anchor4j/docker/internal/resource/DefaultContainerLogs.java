package io.github.cowwoc.anchor4j.docker.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.internal.client.InternalDockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Container.Id;
import io.github.cowwoc.anchor4j.docker.resource.ContainerLogs;
import io.github.cowwoc.anchor4j.docker.resource.ProcessListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultContainerLogs implements ContainerLogs
{
	private final InternalDockerClient client;
	private final Id id;
	private boolean follow;
	private Instant since;
	private Instant until;
	private boolean timestamps;
	private int linesFromEnd = Integer.MAX_VALUE;
	private final Logger log = LoggerFactory.getLogger(DefaultContainerLogs.class);

	/**
	 * Creates a DefaultContainerLogs.
	 *
	 * @param client the client configuration
	 * @param id     the container ID
	 * @throws NullPointerException if {@code id} is null
	 */
	public DefaultContainerLogs(InternalDockerClient client, Id id)
	{
		assert client != null;
		requireThat(id, "id").isNotNull();
		this.client = client;
		this.id = id;
	}

	@Override
	public ContainerLogs follow()
	{
		this.follow = true;
		return this;
	}

	@Override
	public ContainerLogs since(Instant since)
	{
		this.since = since;
		return this;
	}

	@Override
	public ContainerLogs until(Instant until)
	{
		this.until = until;
		return this;
	}

	@Override
	public ContainerLogs timestamps()
	{
		this.timestamps = true;
		return this;
	}

	@Override
	public ContainerLogs linesFromEnd(int linesFromEnd)
	{
		requireThat(linesFromEnd, "linesFromEnd").isNotNegative();
		this.linesFromEnd = linesFromEnd;
		return this;
	}

	@Override
	public ProcessListener apply() throws IOException
	{
		// https://docs.docker.com/reference/cli/docker/container/logs/
		List<String> arguments = new ArrayList<>(11);
		arguments.add("container");
		arguments.add("logs");
		if (follow)
			arguments.add("--follow");
		if (since != null)
		{
			arguments.add("--since");
			arguments.add(String.valueOf(since.toEpochMilli()));
		}
		if (until != null)
		{
			arguments.add("--until");
			arguments.add(String.valueOf(until.toEpochMilli()));
		}
		if (linesFromEnd < Integer.MAX_VALUE)
		{
			arguments.add("--tail");
			arguments.add(String.valueOf(linesFromEnd));
		}
		if (timestamps)
			arguments.add("--timestamps");
		arguments.add(id.getValue());
		ProcessBuilder processBuilder = client.getProcessBuilder(arguments);
		log.debug("Running: {}", processBuilder.command());
		Process process = processBuilder.start();
		return new DefaultProcessListener(process);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder().
			add("id", id).
			add("follow", follow).
			add("since", since).
			add("until", until).
			add("linesFromEnd", linesFromEnd).
			add("timestamps", timestamps).
			toString();
	}

	/**
	 * A container's stdout and stderr log streams.
	 */
	public static final class DefaultProcessListener implements ProcessListener
	{
		private final Process process;

		/**
		 * Creates log streams.
		 *
		 * @param process the docker process
		 */
		private DefaultProcessListener(Process process)
		{
			assert process != null;
			this.process = process;
		}

		@Override
		public InputStream getOutputStream()
		{
			return process.getInputStream();
		}

		@Override
		public BufferedReader getOutputReader()
		{
			return process.inputReader();
		}

		@Override
		public InputStream getErrorStream()
		{
			return process.getErrorStream();
		}

		@Override
		public BufferedReader getErrorReader()
		{
			return process.errorReader();
		}

		@Override
		public int waitFor() throws InterruptedException
		{
			return process.waitFor();
		}
	}
}