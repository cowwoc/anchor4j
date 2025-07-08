package io.github.cowwoc.anchor4j.container.core.resource;

import io.github.cowwoc.anchor4j.container.core.exception.BuilderNotFoundException;
import io.github.cowwoc.anchor4j.container.core.exception.ContextNotFoundException;
import io.github.cowwoc.anchor4j.container.core.exception.UnsupportedExporterException;
import io.github.cowwoc.anchor4j.container.core.internal.resource.BuildXParser;
import io.github.cowwoc.anchor4j.core.internal.util.Exceptions;
import io.github.cowwoc.anchor4j.core.internal.util.Processes;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The default implementation of {@code BuildListener}.
 */
@SuppressWarnings("PMD.MoreThanOneLogger")
public class DefaultBuildListener implements BuildListener
{
	private static final Pattern ERROR_READING_PREFACE = Pattern.compile(".+? .+? http2: server: " +
		"error reading preface from client .+?: file has already been closed\n");
	// Known variants:
	// ERROR: failed to build: resolve : CreateFile C:\Users\Gili\Documents\anchor4j\buildx\src\test\resources\missing: The system cannot find the file specified.
	// ERROR: resolve : lstat /home/runner/work/anchor4j/anchor4j/buildx/src/test/resources/missing: no such file or directory
	// ERROR: failed to build: resolve : lstat /home/runner/work/anchor4j/anchor4j/buildx/src/test/resources/missing: no such file or directory
	private static final List<Pattern> FILE_NOT_FOUND = List.of(Pattern.compile("ERROR: (?:.*?: )?" +
			"resolve : CreateFile (.+?): The system cannot find the file specified\\."),
		Pattern.compile("ERROR: (?:.*?: )?resolve : lstat (.+?): no such file or directory"));
	private static final String DOCKER_DRIVER_DOES_NOT_SUPPORT_MANIFEST_LIST =
		"ERROR: failed to build: docker exporter does not currently support exporting manifest lists";
	private static final String DOCKER_DRIVER_DOES_NOT_SUPPORT_OCI =
		"ERROR: OCI exporter is not supported for the docker driver.";
	// Known variants:
	// ERROR: unable to parse docker host `([^`]+)`
	// ERROR: no valid drivers found: unable to parse docker host `([^`]+)`
	public static final Pattern UNABLE_TO_PARSE_DOCKER_HOST = Pattern.compile("ERROR: (?:.*?: )?" +
		"unable to parse docker host `([^`]+)`");
	private static final Pattern RESOURCE_IN_USE = Pattern.compile("""
		ERROR: failed to build: no valid drivers found: open (.+?): The process cannot access the file because \
		it is being used by another process\\.""");

	/**
	 * The lines returned by the build's standard output stream.
	 */
	protected StringJoiner stdoutJoiner = new StringJoiner("\n");
	/**
	 * The lines returned by the build's standard error stream.
	 */
	protected StringJoiner stderrJoiner = new StringJoiner("\n");
	/**
	 * The exceptions thrown while reading the build's output.
	 */
	protected final BlockingQueue<Throwable> exceptions = new LinkedBlockingQueue<>();
	/**
	 * The process' standard output reader.
	 */
	protected BufferedReader stdoutReader;
	/**
	 * The process' standard error reader.
	 */
	protected BufferedReader stderrReader;
	/**
	 * A blocking operation that waits for the build to complete.
	 */
	protected WaitFor waitFor;
	/**
	 * The thread used to consume the standard output stream.
	 */
	protected Thread stdoutThread;
	/**
	 * The thread used to consume the standard error stream.
	 */
	protected Thread stderrThread;
	/**
	 * Logs the build's standard output stream.
	 */
	protected final Logger stdoutLog = LoggerFactory.getLogger(BuildListener.class.getName() + ".stdout");
	/**
	 * Logs the build's standard error stream.
	 */
	protected final Logger stderrLog = LoggerFactory.getLogger(BuildListener.class.getName() + ".stderr");

	@Override
	public void buildStarted(BufferedReader stdoutReader, BufferedReader stderrReader, WaitFor waitFor)
	{
		this.stdoutJoiner = new StringJoiner("\n");
		this.stderrJoiner = new StringJoiner("\n");
		this.stdoutReader = stdoutReader;
		this.stderrReader = stderrReader;
		this.waitFor = waitFor;
		exceptions.clear();
		Thread parentThread = Thread.currentThread();
		this.stdoutThread = Thread.startVirtualThread(() ->
		{
			stdoutLog.debug("Spawned by thread \"{}\"", parentThread.getName());
			Processes.consume(stdoutReader, exceptions, this::onStdoutLine);
		});
		this.stderrThread = Thread.startVirtualThread(() ->
		{
			stderrLog.debug("Spawned by thread \"{}\"", parentThread.getName());
			Processes.consume(stderrReader, exceptions, this::onStderrLine);
		});
	}

	/**
	 * Invoked after receiving a line from the process' standard output.
	 *
	 * @param line the line
	 */
	protected void onStdoutLine(String line)
	{
		stdoutJoiner.add(line);
		stdoutLog.debug(line);
	}

	/**
	 * Invoked after receiving a line from the process' standard error.
	 *
	 * @param line the line
	 */
	protected void onStderrLine(String line)
	{
		stderrJoiner.add(line);
		// Docker writes build progress to stderr; this does not indicate an error.
		stderrLog.debug(line);
	}

	@Override
	public Output waitUntilBuildCompletes() throws IOException, InterruptedException
	{
		// We have to invoke Thread.join() to ensure that all the data is read. Blocking on Process.waitFor()
		// does not guarantee this.
		stdoutThread.join();
		stderrThread.join();
		IOException exception = Exceptions.combineAsIOException(exceptions);
		if (exception != null)
			throw exception;

		String stdout = stdoutJoiner.toString();
		String stderr = stderrJoiner.toString();
		int exitCode = waitFor.apply();
		return new Output(stdout, stderr, exitCode);
	}

	@Override
	public void buildPassed()
	{
	}

	@Override
	public void buildFailed(CommandResult result) throws IOException
	{
		String stderr = result.stderr();
		Matcher matcher = ERROR_READING_PREFACE.matcher(stderr);
		if (matcher.find())
		{
			// WORKAROUND: https://github.com/docker/buildx/issues/3238
			// Ignore intermittent warning that does not seem to impact the operation. Example:
			// "2025/06/11 15:53:20 http2: server: error reading preface from client //./pipe/dockerDesktopLinuxEngine: file has already been closed"
			stderr = stderr.substring(matcher.end());
		}
		for (Pattern pattern : FILE_NOT_FOUND)
		{
			matcher = pattern.matcher(stderr);
			if (matcher.matches())
				throw new FileNotFoundException(matcher.group(1));
		}
		matcher = BuildXParser.NOT_FOUND.matcher(stderr);
		if (matcher.matches())
			throw new BuilderNotFoundException(matcher.group(1));
		if (stderr.equals(DOCKER_DRIVER_DOES_NOT_SUPPORT_MANIFEST_LIST))
		{
			throw new UnsupportedExporterException("""
				The "docker" driver does not support exporting multi-platform images. Switch to a builder with a \
				driver that does. For more information, see https://docs.docker.com/build/builders/drivers/ and \
				https://docs.docker.com/engine/storage/containerd/.
				""" +
				"exitCode   : " + result.exitCode() + ".\n" +
				"command: " + result.command() + "\n" +
				"directory  : " + result.workingDirectory());
		}
		if (stderr.equals(DOCKER_DRIVER_DOES_NOT_SUPPORT_OCI))
		{
			throw new UnsupportedExporterException("""
				The "docker" driver does not support the OCI exporter. Switch to a builder with a driver that does, \
				or enable the containerd image store. For more information, see \
				https://docs.docker.com/build/builders/drivers/ and \
				https://docs.docker.com/engine/storage/containerd/.
				""" +
				"exitCode   : " + result.exitCode() + ".\n" +
				"command: " + result.command() + "\n" +
				"directory  : " + result.workingDirectory());
		}
		// WORKAROUND: https://github.com/moby/moby/issues/50160
		matcher = UNABLE_TO_PARSE_DOCKER_HOST.matcher(stderr);
		if (matcher.matches())
			throw new ContextNotFoundException(matcher.group(1));
		matcher = RESOURCE_IN_USE.matcher(stderr);
		if (matcher.matches())
		{
			throw new IOException("Failed to build the image because a file is being used by another process.\n" +
				"File     : " + matcher.group(1) + "\n" +
				"exitCode   : " + result.exitCode() + ".\n" +
				"command: " + result.command() + "\n" +
				"directory  : " + result.workingDirectory());
		}
	}

	@Override
	public void buildCompleted() throws IOException
	{
		stdoutReader.close();
		stderrReader.close();
	}
}