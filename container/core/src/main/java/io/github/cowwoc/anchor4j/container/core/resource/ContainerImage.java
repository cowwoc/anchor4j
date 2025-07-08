package io.github.cowwoc.anchor4j.container.core.resource;

import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.id.StringId;

/**
 * A container image.
 *
 * <h2>Terminology</h2>
 * An image's <b>reference</b> is an identifier having the format
 * {@code [HOST[:PORT]/][NAMESPACE/]REPOSITORY[:TAG|@DIGEST]}.
 * <p>
 * Where:
 * <ul>
 *   <li>{@code HOST} refers to the registry host where the image resides. If omitted, the image is
 *   assumed to reside on Docker Hub ({@code docker.io})</li>
 *   <li>{@code PORT} refers to the registry port number. If omitted, the default port number (443 for
 *   HTTPS, 80 for HTTP) is used.</li>
 *   <li>{@code NAMESPACE} typically refers to the user or organization that published the image. If omitted,
 *   it defaults to {@code library}, reserved for Docker Official Images.</li>
 *   <li>{@code REPOSITORY} is the name that groups related images, typically representing a specific
 *   application, service, or component within the given namespace in the registry.</li>
 *   <li>{@code TAG} is the version or variant of the image.</li>
 *   <li>{@code DIGEST} is a SHA256 hash of the image's content that uniquely and immutably identifies it.
 *   The digest ensures content integrity - even if tags change or move, the digest always points to the
 *   exact image.</li>
 *   <li>An image reference includes either a tag or a digest, but not both. If both the tag and digest are
 *   omitted, a default tag of {@code latest} is used.</li>
 * </ul>
 * <p>
 * An image's <b>ID</b> is an automatically assigned identifier that is unique per host and may be equal to
 * the image digest.
 * <p>
 * <b>Thread Safety</b>: Implementations must be immutable and thread-safe.
 */
public interface ContainerImage
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the image's ID or {@link ContainerImage reference}
	 * @return the type-safe identifier for the resource
	 * @throws NullPointerException     if {@code value} is null
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(String value)
	{
		return new Id(value);
	}

	/**
	 * Returns the image's ID or reference.
	 *
	 * @return the ID or reference
	 */
	Id getId();

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
}