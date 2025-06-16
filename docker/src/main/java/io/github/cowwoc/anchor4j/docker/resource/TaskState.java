package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.Objects;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A snapshot of a task's state.
 * <p>
 * <b>Thread Safety</b>: This class is immutable and thread-safe.
 */
public final class TaskState
{
	private final Docker client;
	private final String id;
	private final String name;
	private final State state;

	/**
	 * Creates a reference to a task.
	 *
	 * @param client the client configuration
	 * @param id     the task's ID
	 * @param name   the task's name
	 * @param state  the task's state
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if any of the arguments contain whitespace or are empty
	 */
	public TaskState(Docker client, String id, String name, State state)
	{
		requireThat(client, "client").isNotNull();
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();
		requireThat(state, "state").isNotNull();
		this.client = client;
		this.id = id;
		this.name = name;
		this.state = state;
	}

	/**
	 * Returns the task's ID.
	 *
	 * @return the ID
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Returns the task's name.
	 *
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the task's state.
	 *
	 * @return the state
	 */
	public State getState()
	{
		return state;
	}

	/**
	 * Reloads the task's state.
	 *
	 * @return the updated state
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@CheckReturnValue
	public TaskState reload() throws IOException, InterruptedException
	{
		return client.getTaskState(id);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, state);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof TaskState other && other.id.equals(id) && other.name.equals(name) &&
			other.state.equals(state);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(TaskState.class).
			add("id", id).
			add("name", name).
			add("state", state).
			toString();
	}

	/**
	 * A task's states.
	 */
	public enum State
	{
		// Based on https://github.com/moby/swarmkit/blob/8c19597365549f6966a5021d2ea46810099aae28/api/types.proto#L516
		/**
		 * The task has been created but has not yet been assigned to any node.
		 */
		NEW,
		/**
		 * The task has been submitted to the scheduler, but no node has been selected yet.
		 */
		PENDING,
		/**
		 * The task has been assigned to a node but not yet acknowledged by it.
		 */
		ASSIGNED,
		/**
		 * The node has accepted the task and will begin preparing it.
		 */
		ACCEPTED,
		/**
		 * The task is preparing its environment (e.g., downloading images, setting up mounts or networks).
		 */
		PREPARING,
		/**
		 * The task is fully prepared and ready to start execution.
		 */
		READY,
		/**
		 * The task's container is starting.
		 */
		STARTING,
		/**
		 * The task's container is actively running.
		 */
		RUNNING,
		/**
		 * The task has finished execution successfully (zero exit code).
		 */
		COMPLETE,
		/**
		 * The task has been shut down due to update, scaling, or other scheduler decisions.
		 */
		SHUTDOWN,
		/**
		 * The task has failed due to an error or non-zero exit code.
		 */
		FAILED,
		/**
		 * The task was rejected by the scheduler or an agent (e.g., due to constraints or failures).
		 */
		REJECTED,
		/**
		 * The task is marked for deletion and will be removed shortly.
		 */
		REMOVE,
		/**
		 * The task was orphaned (e.g., due to node unavailability or loss of manager connectivity).
		 */
		ORPHANED
	}
}