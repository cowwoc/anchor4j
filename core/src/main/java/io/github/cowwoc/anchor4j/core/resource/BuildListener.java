package io.github.cowwoc.anchor4j.core.resource;

import java.io.BufferedReader;
import java.io.IOException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A class that observes and reacts to an image build operation.
 * <p>
 * Implementations must support repeated invocations of the build process, as retries may occur due to
 * intermittent failures.
 */
public interface BuildListener
{
	/**
	 * Invoked after the build starts.
	 * <p>
	 * This method may be called multiple times if the build is retried due to intermittent failures.
	 *
	 * @param stdoutReader the standard output stream of the command
	 * @param stderrReader the standard error stream of the command
	 * @param waitFor      a blocking operation that waits for the process to terminate and returns its exit
	 *                     code
	 * @throws NullPointerException if any of the arguments are null
	 */
	void buildStarted(BufferedReader stdoutReader, BufferedReader stderrReader, WaitFor waitFor);

	/**
	 * Waits until the build completes.
	 *
	 * @return the build's output
	 * @throws IOException          if an error occurs while reading the build's standard output or error
	 * @throws InterruptedException if the thread is interrupted before the operation completes streams
	 */
	Output waitUntilBuildCompletes() throws IOException, InterruptedException;

	/**
	 * Invoked if the build succeeds.
	 */
	void buildPassed();

	/**
	 * Invoked if the build fails.
	 *
	 * @param result the result of executing the build
	 * @throws IOException if the build failure is expected
	 */
	void buildFailed(CommandResult result) throws IOException;

	/**
	 * Invoked after {@link #buildPassed()} or {@link #buildFailed(CommandResult)}.
	 *
	 * @throws IOException if an error occurs while closing the build's standard output or error streams
	 */
	void buildCompleted() throws IOException;

	/**
	 * The build's output.
	 *
	 * @param stdout   the full contents of the build's standard output stream
	 * @param stderr   the full contents of the build's standard error stream
	 * @param exitCode the exit code returned by the build process
	 */
	record Output(String stdout, String stderr, int exitCode)
	{
		/**
		 * Creates an Output.
		 *
		 * @param stdout   the full contents of the build's standard output stream
		 * @param stderr   the full contents of the build's standard error stream
		 * @param exitCode the exit code returned by the build process
		 * @throws NullPointerException if any of the arguments are null
		 */
		public Output
		{
			requireThat(stdout, "stdout").isNotNull();
			requireThat(stderr, "stderr").isNotNull();
		}
	}
}