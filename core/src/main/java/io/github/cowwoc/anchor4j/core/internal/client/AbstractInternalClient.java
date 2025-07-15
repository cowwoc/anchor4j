package io.github.cowwoc.anchor4j.core.internal.client;

import io.github.cowwoc.anchor4j.core.client.Client;
import io.github.cowwoc.anchor4j.core.migration.ResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Common implementation shared by all {@code InternalClient}s.
 */
@SuppressWarnings("PMD.MoreThanOneLogger")
public abstract class AbstractInternalClient implements InternalClient
{
	private final static Duration SLEEP_DURATION = Duration.ofMillis(100);
	protected Duration retryTimeout = Duration.ofSeconds(30);
	private final Map<ResourceId, Object> sourceState = new HashMap<>();
	private final Map<ResourceId, Object> targetState = new HashMap<>();
	@SuppressWarnings("this-escape")
	protected final Logger log = LoggerFactory.getLogger(AbstractInternalClient.class);

	/**
	 * Creates an AbstractInternalClient.
	 */
	protected AbstractInternalClient()
	{
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
			catch (IllegalStateException e)
			{
				if (e.getClass().getName().
					equals("io.github.cowwoc.anchor4j.container.core.exception.UnsupportedExporterException"))
				{
					// Surprisingly, the following error occurs intermittently under load:
					//
					// ERROR: failed to build: docker exporter does not currently support exporting manifest lists
					if (!sleepBeforeRetry(deadline, e))
						throw e;
				}
				else
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

	/**
	 * Returns a resource's expected state prior to applying a migration.
	 *
	 * @param id the resource ID
	 * @return null if no match is found
	 */
	public Object getSourceState(ResourceId id)
	{
		return sourceState.get(id);
	}

	/**
	 * Sets a resource's expected state for after a migration is applied.
	 *
	 * @param id    the resource ID
	 * @param state the resource's state, or {@code null} if the resource is destroyed
	 */
	public void setTargetState(ResourceId id, Object state)
	{
		assert id != null;
		targetState.put(id, state);
	}
}