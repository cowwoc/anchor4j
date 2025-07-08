package io.github.cowwoc.anchor4j.digitalocean.project.resource;

import io.github.cowwoc.anchor4j.core.id.StringId;

import java.io.IOException;
import java.time.Instant;

/**
 * A group of resources.
 */
public interface Project
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the server-side identifier
	 * @return the type-safe identifier for the resource
	 * @throws NullPointerException     if {@code value} is null
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(String value)
	{
		return new Id(value);
	}

	/**
	 * Returns the project's ID.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * Returns the name of the project.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the UUID of the project owner.
	 *
	 * @return the UUID
	 */
	String getOwnerUuid();

	/**
	 * Returns the ID of the project owner.
	 *
	 * @return the ID
	 */
	int getOwnerId();

	/**
	 * Returns a description of the project.
	 *
	 * @return the description
	 */
	String getDescription();

	/**
	 * Returns the purpose of the project.
	 *
	 * @return the purpose
	 */
	String getPurpose();

	/**
	 * Returns the project's environment.
	 *
	 * @return the environment
	 */
	Environment getEnvironment();

	/**
	 * Determines if this is the default project.
	 *
	 * @return {@code true} if this is the default project
	 */
	boolean isDefault();

	/**
	 * Returns the time the project was created.
	 *
	 * @return the time
	 */
	Instant getCreatedAt();

	/**
	 * Returns the time the project was last updated.
	 *
	 * @return the time
	 */
	Instant getUpdatedAt();

	/**
	 * Destroys the project.
	 *
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	void destroy() throws IOException, InterruptedException;

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value a server-side identifier
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
		 */
		private Id(String value)
		{
			super(value);
		}
	}

	/**
	 * A project's deployment environment.
	 */
	enum Environment
	{
		/**
		 * The development environment.
		 */
		DEVELOPMENT,
		/**
		 * The staging environment.
		 */
		STAGING,
		/**
		 * The production environment.
		 */
		PRODUCTION
	}
}