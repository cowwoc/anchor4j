package io.github.cowwoc.anchor4j.digitalocean.compute.internal.util;

import java.time.Duration;
import java.time.Instant;

/**
 * The maximum amount of time that an operation may execute for.
 */
public final class TimeLimit
{
	private final Duration timeQuota;
	private final Instant deadline;

	/**
	 * Creates a new time limit.
	 *
	 * @param timeQuota the maximum amount of time that the operation may execute
	 * @throws NullPointerException if {@code timeQuota} is null
	 */
	public TimeLimit(Duration timeQuota)
	{
		this.timeQuota = timeQuota;
		this.deadline = Instant.now().plus(timeQuota);
	}

	/**
	 * Returns the maximum amount of time that the operation may execute. Once the time elapses, the request
	 * must abort execution and return an error to the client.
	 *
	 * @return the maximum amount of time that the operation may execute
	 */
	public Duration getTimeQuota()
	{
		return timeQuota;
	}

	/**
	 * Returns the time left for this operation's execution.
	 *
	 * @return the time left for this operation's execution
	 */
	public Duration getTimeLeft()
	{
		return Duration.between(Instant.now(), deadline);
	}
}