package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.resource.Context.Id;

import java.util.function.Predicate;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.that;

/**
 * The properties used by the predicate in {@link DockerClient#getContexts(Predicate)}.
 *
 * @param id          the context's ID
 * @param current     {@code true} if this is the current user context
 * @param description a description of the context
 * @param endpoint    the configuration of the target Docker Engine
 * @param error       an explanation of why the context is unavailable, or an empty string if the context is
 *                    available
 */
public record ContextElement(Id id, boolean current, String description, String endpoint,
                             String error)
{
	/**
	 * Creates a context element.
	 *
	 * @param id          the context's ID
	 * @param current     {@code true} if this is the current user context
	 * @param description a description of the context
	 * @param endpoint    the configuration of the target Docker Engine
	 * @param error       an explanation of why the context is unavailable, or an empty string if the context is
	 *                    available
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code description} or {@code error} contain leading or trailing
	 *                                    whitespace.</li>
	 *                                    <li>{@code endpoint} contains whitespace or is empty.</li>
	 *                                  </ul>
	 */
	public ContextElement
	{
		assert id != null;
		assert that(description, "description").isStripped().elseThrow();
		assert that(endpoint, "endpoint").doesNotContainWhitespace().isNotEmpty().elseThrow();
		assert that(error, "error").isStripped().elseThrow();
	}
}