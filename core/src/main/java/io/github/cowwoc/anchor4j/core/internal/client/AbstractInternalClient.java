package io.github.cowwoc.anchor4j.core.internal.client;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.cowwoc.anchor4j.core.client.Client;
import io.github.cowwoc.anchor4j.core.exception.UnsupportedExporterException;
import io.github.cowwoc.anchor4j.core.internal.resource.BuildXParser;
import io.github.cowwoc.anchor4j.core.internal.resource.DefaultBuilder;
import io.github.cowwoc.anchor4j.core.internal.resource.DefaultBuilderCreator;
import io.github.cowwoc.anchor4j.core.internal.resource.DefaultCoreImage;
import io.github.cowwoc.anchor4j.core.internal.resource.DefaultCoreImageBuilder;
import io.github.cowwoc.anchor4j.core.internal.util.Exceptions;
import io.github.cowwoc.anchor4j.core.resource.Builder;
import io.github.cowwoc.anchor4j.core.resource.BuilderCreator;
import io.github.cowwoc.anchor4j.core.resource.BuilderState;
import io.github.cowwoc.anchor4j.core.resource.BuilderState.Status;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.core.resource.CoreImage;
import io.github.cowwoc.anchor4j.core.resource.CoreImageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;

import static io.github.cowwoc.anchor4j.core.internal.resource.AbstractParser.ACCESS_DENIED;
import static io.github.cowwoc.anchor4j.core.internal.resource.AbstractParser.CONNECTION_RESET;
import static io.github.cowwoc.anchor4j.core.internal.resource.AbstractParser.FILE_LOCKED;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Common implementation shared by all {@code InternalClient}s.
 */
@SuppressWarnings("PMD.MoreThanOneLogger")
public abstract class AbstractInternalClient implements InternalClient
{
	private final static ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocate(0);
	private final static Duration SLEEP_DURATION = Duration.ofMillis(100);
	/**
	 * The path of the command-line executable.
	 */
	protected final Path executable;
	protected Duration retryTimeout = Duration.ofSeconds(30);
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	@SuppressWarnings("this-escape")
	private final BuildXParser buildXParser = new BuildXParser(this);
	private final Logger log = LoggerFactory.getLogger(AbstractInternalClient.class);
	private final Logger stdoutLog = LoggerFactory.getLogger(AbstractInternalClient.class.getName() +
		".stdout");
	private final Logger stderrLog = LoggerFactory.getLogger(AbstractInternalClient.class.getName() +
		".stderr");

	/**
	 * Creates an AbstractInternalClient.
	 *
	 * @param executable the path of the command-line executable
	 * @throws NullPointerException     if {@code executable} is null
	 * @throws IllegalArgumentException if the path referenced by {@code executable} does not exist or is not an
	 *                                  executable file
	 * @throws IOException              if an I/O error occurs while reading {@code executable}'s attributes
	 */
	public AbstractInternalClient(Path executable) throws IOException
	{
		requireThat(executable, "executable").exists().isRegularFile().isExecutable();
		this.executable = executable;
	}

	@Override
	public JsonMapper getJsonMapper()
	{
		return jsonMapper;
	}

	@Override
	public Client retryTimeout(Duration duration)
	{
		requireThat(duration, "duration").isNotNull();
		retryTimeout = duration;
		return this;
	}

	@Override
	public Duration getRetryTimeout()
	{
		return retryTimeout;
	}

	@Override
	public <V> V retry(Operation<V> operation) throws IOException, InterruptedException
	{
		try
		{
			return retry(operation, Instant.now().plus(getRetryTimeout()));
		}
		catch (TimeoutException e)
		{
			throw new AssertionError("An operation without a timeout threw a TimeoutException", e);
		}
	}

	@Override
	public <V> V retry(Operation<V> operation, Instant deadline)
		throws IOException, InterruptedException, TimeoutException
	{
		while (true)
		{
			try
			{
				return operation.run(deadline);
			}
			catch (FileNotFoundException e)
			{
				// Failures that are assumed to be non-intermittent
				throw e;
			}
			catch (IOException e)
			{
				// WORKAROUND: https://github.com/moby/moby/issues/50160
				if (!sleepBeforeRetry(deadline, e))
					throw e;
			}
			catch (UnsupportedExporterException e)
			{
				// Surprisingly, the following error occurs intermittently under load:
				//
				// ERROR: failed to build: docker exporter does not currently support exporting manifest lists
				if (!sleepBeforeRetry(deadline, e))
					throw e;
			}
		}
	}

	/**
	 * Checks if a timeout occurred.
	 *
	 * @param deadline the absolute time by which the operation must succeed. The method will retry failed
	 *                 operations while the current time is before this value.
	 * @param t        the exception that was thrown
	 * @return {@code true} if the operation may be retried
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean sleepBeforeRetry(Instant deadline, Throwable t) throws InterruptedException
	{
		Instant nextRetry = Instant.now().plus(SLEEP_DURATION);
		if (nextRetry.isAfter(deadline))
			return false;
		Thread.sleep(SLEEP_DURATION);
		log.debug("Retrying after sleep", t);
		return true;
	}

	/**
	 * Checks if a timeout occurred.
	 *
	 * @param deadline the absolute time by which the operation must succeed. The method will retry failed
	 *                 operations while the current time is before this value.
	 * @return {@code true} if the operation may be retried
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	protected boolean sleepBeforeRetry(Instant deadline) throws InterruptedException
	{
		Instant nextRetry = Instant.now().plus(SLEEP_DURATION);
		if (nextRetry.isAfter(deadline))
			return false;
		Thread.sleep(SLEEP_DURATION);
		log.debug("Retrying after sleep");
		return true;
	}

	@Override
	public CommandResult run(List<String> arguments, Instant deadline) throws IOException, InterruptedException
	{
		return run(arguments, EMPTY_BYTE_BUFFER, deadline);
	}

	@Override
	public CommandResult run(List<String> arguments, ByteBuffer stdin, Instant deadline)
		throws IOException, InterruptedException
	{
		ProcessBuilder processBuilder = getProcessBuilder(arguments);
		log.debug("Running: {}", processBuilder.command());
		Process process = processBuilder.start();
		StringJoiner stdoutJoiner = new StringJoiner("\n");
		StringJoiner stderrJoiner = new StringJoiner("\n");
		BlockingQueue<Throwable> exceptions = new LinkedBlockingQueue<>();

		writeIntoStdin(stdin, process, exceptions);
		Thread parentThread = Thread.currentThread();
		try (BufferedReader stdoutReader = process.inputReader();
		     BufferedReader stderrReader = process.errorReader())
		{
			Thread stdoutThread = Thread.startVirtualThread(() ->
			{
				stdoutLog.debug("Spawned by thread \"{}\"", parentThread.getName());
				Processes.consume(stdoutReader, exceptions, line ->
				{
					stdoutJoiner.add(line);
					stdoutLog.debug(line);
				});
			});
			Thread stderrThread = Thread.startVirtualThread(() ->
			{
				stderrLog.debug("Spawned by thread \"{}\"", parentThread.getName());
				Processes.consume(stderrReader, exceptions, line ->
				{
					stderrJoiner.add(line);
					stderrLog.debug(line);
				});
			});

			// We have to invoke Thread.join() to ensure that all the data is read. Blocking on Process.waitFor()
			// does not guarantee this.
			stdoutThread.join();
			stderrThread.join();
			int exitCode = process.waitFor();
			IOException exception = Exceptions.combineAsIOException(exceptions);
			if (exception != null)
				throw exception;
			String stdout = stdoutJoiner.toString();
			String stderr = stderrJoiner.toString();

			Path workingDirectory = Processes.getWorkingDirectory(processBuilder);
			CommandResult result = new CommandResult(processBuilder.command(), workingDirectory, stdout, stderr,
				exitCode);
			if (exitCode != 0)
				commandFailed(result);
			return result;
		}
	}

	@Override
	public void commandFailed(CommandResult result) throws IOException
	{
		String stderr = result.stderr();
		Matcher matcher = CONNECTION_RESET.matcher(stderr);
		if (matcher.matches())
			throw new IOException("Connection reset trying to connect to " + matcher.group(1));
		if (!Processes.isWindows())
			return;
		matcher = FILE_LOCKED.matcher(stderr);
		if (matcher.matches())
			throw new IOException("File locked by another process: " + matcher.group(1));
		matcher = ACCESS_DENIED.matcher(stderr);
		if (matcher.matches())
			throw new IOException("File locked by another process: " + matcher.group(1));
	}

	/**
	 * Writes data into a process' {@code stdin} stream.
	 *
	 * @param bytes      the bytes to write
	 * @param process    the process to write into
	 * @param exceptions the queue to add any thrown exceptions to
	 */
	private static void writeIntoStdin(ByteBuffer bytes, Process process, BlockingQueue<Throwable> exceptions)
	{
		if (bytes.hasRemaining())
		{
			Thread.startVirtualThread(() ->
			{
				try (OutputStream os = process.getOutputStream();
				     WritableByteChannel stdin = Channels.newChannel(os))
				{
					while (bytes.hasRemaining())
						stdin.write(bytes);
				}
				catch (IOException | RuntimeException e)
				{
					exceptions.add(e);
				}
			});
		}
	}

	@Override
	public BuildXParser getBuildXParser()
	{
		return buildXParser;
	}

	@Override
	public Builder builder(String name)
	{
		return new DefaultBuilder(this, name);
	}

	@Override
	public BuilderState getBuilderState() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/buildx/inspect/
		List<String> arguments = new ArrayList<>(2);
		arguments.add("buildx");
		arguments.add("inspect");
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getBuildXParser().getState(result);
	}

	@Override
	public BuilderCreator createBuilder()
	{
		return new DefaultBuilderCreator(this);
	}

	@Override
	public BuilderState getBuilderState(String name) throws IOException, InterruptedException
	{
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/buildx/inspect/
		List<String> arguments = new ArrayList<>(3);
		arguments.add("buildx");
		arguments.add("inspect");
		arguments.add(name);
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getBuildXParser().getState(result);
	}

	@Override
	@SuppressWarnings("BusyWait")
	public BuilderState waitUntilBuilderIsReady(Instant deadline)
		throws IOException, InterruptedException, TimeoutException
	{
		while (true)
		{
			BuilderState builder = getBuilderState();
			if (builder != null && builder.getStatus() == Status.RUNNING)
				return builder;
			Instant now = Instant.now();
			if (builder == null)
			{
				log.debug("builder == null");
				if (now.isAfter(deadline))
					throw new TimeoutException("Default builder not found");
			}
			else
			{
				log.debug("builder.status: {}", builder.getStatus());
				if (now.isAfter(deadline))
				{
					StringBuilder message = new StringBuilder(
						"Default builder " + builder.getName() + " has a state of " +
							builder.getStatus());
					if (builder.getStatus() == Status.ERROR)
					{
						message.append("\n" +
							"Error: ").append(builder.getError());
					}
					throw new TimeoutException(message.toString());
				}
			}
			Thread.sleep(100);
		}
	}

	@Override
	public Set<String> getSupportedBuildPlatforms() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/buildx/inspect/
		List<String> arguments = List.of("buildx", "inspect");
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getBuildXParser().getSupportedBuildPlatforms(result);
	}

	@Override
	public CoreImage image(String id)
	{
		return new DefaultCoreImage(id);
	}

	@Override
	public CoreImageBuilder buildImage()
	{
		return new DefaultCoreImageBuilder(this);
	}
}