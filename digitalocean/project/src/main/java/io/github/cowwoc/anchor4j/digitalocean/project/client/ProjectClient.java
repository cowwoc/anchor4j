package io.github.cowwoc.anchor4j.digitalocean.project.client;

import io.github.cowwoc.anchor4j.digitalocean.core.client.DigitalOceanClient;
import io.github.cowwoc.anchor4j.digitalocean.project.internal.client.DefaultProjectClient;
import io.github.cowwoc.anchor4j.digitalocean.project.resource.Project;
import io.github.cowwoc.anchor4j.digitalocean.project.resource.Project.Id;

import java.io.IOException;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A DigitalOcean project client.
 */
public interface ProjectClient extends DigitalOceanClient
{
	/**
	 * Returns a client.
	 *
	 * @return the client
	 * @throws IOException if an I/O error occurs while building the client
	 */
	static ProjectClient build() throws IOException
	{
		return new DefaultProjectClient();
	}

	/**
	 * Returns all the projects.
	 *
	 * @return an empty set if no match is found
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Set<Project> getProjects() throws IOException, InterruptedException;

	/**
	 * Returns the first project that matches a predicate.
	 *
	 * @param predicate the predicate
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code predicate} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Project getProject(Predicate<Project> predicate) throws IOException, InterruptedException;

	/**
	 * Looks up a project by its ID.
	 *
	 * @param id the ID
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code id} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Project getProject(Id id) throws IOException, InterruptedException;

	/**
	 * Returns the default project.
	 *
	 * @return null if no match is found
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted while waiting for a response. This can happen
	 *                              due to shutdown signals.
	 */
	Project getDefaultProject() throws IOException, InterruptedException;
}