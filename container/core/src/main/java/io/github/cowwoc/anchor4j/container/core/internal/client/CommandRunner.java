package io.github.cowwoc.anchor4j.container.core.internal.client;

import io.github.cowwoc.anchor4j.core.internal.util.Exceptions;
import io.github.cowwoc.anchor4j.core.internal.util.Processes;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.StringJoiner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Runs a command.
 */
@SuppressWarnings("PMD.MoreThanOneLogger")
public final class CommandRunner
{
	private final static ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocate(0);
	private final ProcessBuilder processBuilder;
	private ByteBuffer stdin = EMPTY_BYTE_BUFFER;
	private Predicate<String> terminateOnStdout = _ -> false;
	private FailureHandler failureHandler;
	private final Logger log = LoggerFactory.getLogger(CommandRunner.class);
	private final Logger stdoutLog = LoggerFactory.getLogger(CommandRunner.class.getName() + ".stdout");
	private final Logger stderrLog = LoggerFactory.getLogger(CommandRunner.class.getName() + ".stderr");

	/**
	 * Creates a new CommandRunner.
	 *
	 * @param processBuilder the {@code ProcessBuilder} to run the command with
	 * @throws NullPointerException if {@code processBuilder} is null
	 */
	public CommandRunner(ProcessBuilder processBuilder)
	{
		requireThat(processBuilder, "processBuilder").isNotNull();
		this.processBuilder = processBuilder;
	}

	/**
	 * Sets the bytes to pass into the command's stdin stream.
	 *
	 * @param stdin the input
	 * @return this
	 */
	public CommandRunner stdin(ByteBuffer stdin)
	{
		requireThat(stdin, "stdin").isNotNull();
		this.stdin = stdin;
		return this;
	}

	/**
	 * Specifies a function that consumes stdout lines and returns {@code true} if the process should be
	 * terminated.
	 *
	 * @param terminateOnStdout the function
	 * @return this
	 */
	public CommandRunner terminateOnStdout(Predicate<String> terminateOnStdout)
	{
		this.terminateOnStdout = terminateOnStdout;
		return this;
	}

	/**
	 * Specifies the function to invoke if the command fails.
	 *
	 * @param failureHandler the failure handler
	 */
	public CommandRunner failureHandler(FailureHandler failureHandler)
	{
		this.failureHandler = failureHandler;
		return this;
	}

	/**
	 * Runs a command and returns its output.
	 *
	 * @return the output of the command
	 * @throws IOException          if the executable could not be found
	 * @throws InterruptedException if the thread was interrupted before the operation completed
	 */
	public CommandResult apply() throws IOException, InterruptedException
	{
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
					if (terminateOnStdout.test(line))
						process.destroy();
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
				failureHandler.onFailure(result);
			return result;
		}
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

	/**
	 * Invoked if the command fails.
	 */
	@FunctionalInterface
	public interface FailureHandler
	{
		/**
		 * Specifies the function to invoke if the command returns a non-zero exit code.
		 *
		 * @param result the result of executing a command
		 * @throws IOException if the command should fail
		 */
		void onFailure(CommandResult result) throws IOException;
	}
}