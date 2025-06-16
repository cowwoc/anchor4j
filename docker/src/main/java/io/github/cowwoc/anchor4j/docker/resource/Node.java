package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.resource.NodeState.Role;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * A snapshot of a node's state.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public interface Node
{
	/**
	 * Returns the node's id.
	 *
	 * @return the id
	 */
	String getId();

	/**
	 * Looks up the node's state.
	 *
	 * @return the state
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	NodeState getState() throws IOException, InterruptedException;

	/**
	 * Lists the tasks that are assigned to the node.
	 * <p>
	 * This includes tasks in active lifecycle states such as {@code New}, {@code Allocated}, {@code Pending},
	 * {@code Assigned}, {@code Accepted}, {@code Preparing}, {@code Ready}, {@code Starting}, and
	 * {@code Running}. These states represent tasks that are in progress or actively running and are reliably
	 * returned by this command.
	 * <p>
	 * However, tasks that have reached a terminal state—such as {@code Complete}, {@code Failed}, or
	 * {@code Shutdown}— are often pruned by Docker shortly after they exit, and are therefore not guaranteed to
	 * appear in the results, even if they completed very recently.
	 * <p>
	 * Note that Docker prunes old tasks aggressively from this command, so {@link Service#listTasks()} will
	 * often provide more comprehensive historical data by design.
	 *
	 * @return the tasks
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	List<TaskState> listTasks() throws IOException, InterruptedException;

	/**
	 * Begins gracefully removing tasks from this node and redistribute them to other active nodes.
	 *
	 * @return the updated node
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	Node drain() throws IOException, InterruptedException;

	/**
	 * Sets the role of a node.
	 *
	 * @param role     the new role
	 * @param deadline the absolute time by which the node's role must change. The method will poll the node's
	 *                 state while the current time is before this value.
	 * @return the updated node
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if a node attempts to modify its own role
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 * @throws TimeoutException         if the deadline expires before the operation succeeds
	 */
	Node setRole(Role role, Instant deadline) throws IOException, InterruptedException, TimeoutException;

	/**
	 * Removes a node from the swarm.
	 *
	 * @return an node remover
	 */
	@CheckReturnValue
	NodeRemover remover();
}