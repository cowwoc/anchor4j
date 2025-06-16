package io.github.cowwoc.anchor4j.core.client;

import io.github.cowwoc.anchor4j.core.resource.Builder;
import io.github.cowwoc.anchor4j.core.resource.BuilderCreator;
import io.github.cowwoc.anchor4j.core.resource.BuilderState;
import io.github.cowwoc.anchor4j.core.resource.CoreImage;
import io.github.cowwoc.anchor4j.core.resource.CoreImageBuilder;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Code common to all clients.
 */
public interface Client
{
	/**
	 * Sets the maximum duration to retry a command that fails due to intermittent {@code IOException}s. The
	 * default is 10 seconds.
	 * <p>
	 * If the timeout is exceeded, the command fails with the last encountered {@code IOException}.
	 *
	 * @param duration the timeout
	 * @return this
	 * @throws NullPointerException if {@code duration} is null
	 */
	Client retryTimeout(Duration duration);

	/**
	 * Returns a reference to a builder.
	 *
	 * @param name the name of the builder
	 * @return the builder
	 */
	Builder builder(String name);

	/**
	 * Looks up the default builder.
	 *
	 * @return the builder, or {@code null} if no match is found
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	BuilderState getBuilderState() throws IOException, InterruptedException;

	/**
	 * Looks up a builder by its name.
	 *
	 * @param name the name of the builder
	 * @return the builder, or {@code null} if no match is found
	 * @throws NullPointerException     if {@code name} is null
	 * @throws IllegalArgumentException if {@code name} contains whitespace or is empty
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	BuilderState getBuilderState(String name) throws IOException, InterruptedException;

	/**
	 * Creates a builder.
	 *
	 * @return a builder creator
	 */
	@CheckReturnValue
	BuilderCreator createBuilder();

	/**
	 * Blocks until the default builder is reachable and has a {@code RUNNING} state.
	 *
	 * @param deadline the absolute time by which the builder must be ready. The method will poll the builder's
	 *                 state while the current time is before this value.
	 * @return the builder
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 * @throws TimeoutException     if the deadline expires before the operation succeeds
	 */
	BuilderState waitUntilBuilderIsReady(Instant deadline)
		throws IOException, InterruptedException, TimeoutException;

	/**
	 * Returns the platforms that images can be built for.
	 *
	 * @return the platforms
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	Set<String> getSupportedBuildPlatforms() throws IOException, InterruptedException;

	/**
	 * Returns a reference to an image.
	 *
	 * @param id the image's ID or {@link CoreImage reference}
	 * @return the image
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id}:
	 *                                  <ul>
	 *                                    <li>contains whitespace or is empty.</li>
	 *                                    <li>contains uppercase letters.</li>
	 *                                  </ul>
	 */
	CoreImage image(String id);

	/**
	 * Builds an image.
	 *
	 * @return an image builder
	 */
	@CheckReturnValue
	CoreImageBuilder buildImage();
}