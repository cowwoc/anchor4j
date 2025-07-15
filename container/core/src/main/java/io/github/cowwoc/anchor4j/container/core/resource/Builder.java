package io.github.cowwoc.anchor4j.container.core.resource;

import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.id.StringId;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.List;

/**
 * Represents a service that builds images.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public interface Builder
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
	 * Returns the build's ID.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * Returns the name of the builder.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the nodes that the builder is on.
	 *
	 * @return the nodes
	 */
	List<Node> getNodes();

	/**
	 * Returns the driver used by the builder.
	 *
	 * @return the driver
	 * @see <a href="https://docs.docker.com/build/builders/drivers/">Build drivers</a>
	 */
	Driver getDriver();

	/**
	 * Reloads the builder.
	 *
	 * @return the updated builder
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	@CheckReturnValue
	Builder reload() throws IOException, InterruptedException;

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value the name of the builder. The value must start with a letter, or digit, or underscore, and
		 *              may be followed by additional characters consisting of letters, digits, underscores,
		 *              periods or hyphens.
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value}'s format is invalid
		 */
		private Id(String value)
		{
			super(value);
			ParameterValidator.validateName(value, "value");
		}
	}

	/**
	 * Represents a node that the builder is on.
	 */
	interface Node
	{
		/**
		 * Creates a new ID.
		 *
		 * @param value the node's ID
		 * @return the type-safe identifier for the resource
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
		 */
		static Node.Id id(String value)
		{
			return new Node.Id(value);
		}

		/**
		 * Returns the node's ID.
		 *
		 * @return the ID
		 */
		Id getId();

		/**
		 * Returns the name of the node
		 *
		 * @return the name
		 */
		String getName();

		/**
		 * Returns the status of the node
		 *
		 * @return the status
		 */
		Status getStatus();

		/**
		 * Returns an explanation of the builder's error status.
		 *
		 * @return or an empty string if absent
		 */
		String getError();

		/**
		 * A type-safe identifier for this type of resource.
		 * <p>
		 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
		 * of IDs belonging to another class.
		 */
		final class Id extends StringId
		{
			/**
			 * @param value the image's ID or {@link ContainerImage reference}
			 * @throws NullPointerException     if {@code value} is null
			 * @throws IllegalArgumentException if {@code value}'s format is invalid
			 */
			private Id(String value)
			{
				super(value);
				ParameterValidator.validateImageIdOrReference(value, "value");
			}
		}

		/**
		 * Represents the status of a builder on a node.
		 */
		enum Status
		{
			/**
			 * The builder is defined but has not been created yet.
			 * <p>
			 * For example, this status can occur before the BuildKit image has been pulled locally and the builder
			 * instance needs to be initialized.
			 */
			INACTIVE,
			/**
			 * The builder is in the process of starting up. Resources are being initialized, but it is not yet
			 * ready to accept build jobs.
			 */
			STARTING,
			/**
			 * The builder is up and ready to accept jobs.
			 */
			RUNNING,
			/**
			 * The builder is in the process of shutting down. Active jobs may still be completing.
			 */
			STOPPING,
			/**
			 * The builder exists but is not currently running.
			 */
			STOPPED,
			/**
			 * The builder is unavailable due to an error.
			 */
			ERROR
		}
	}

	/**
	 * Represents the type of build driver responsible for executing build processes.
	 * <p>
	 * Drivers define how and where builds are performed. For example, a build may run directly on the local
	 * Docker engine, inside a Docker container, in a Kubernetes cluster, or on a remote server.
	 */
	enum Driver
	{
		/**
		 * Use the local Docker engine to execute builds.
		 */
		DOCKER,

		/**
		 * Use a Docker container as the build environment.
		 */
		DOCKER_CONTAINER,

		/**
		 * Use a Kubernetes cluster to orchestrate builds.
		 */
		KUBERNETES,

		/**
		 * Use a remote build server or service.
		 */
		REMOTE
	}
}