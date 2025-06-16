package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.client.Docker;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * An element returned by {@link Docker#listContexts()}.
 *
 * @param context     the context
 * @param current     {@code true} if this is the current user context
 * @param description a description of the context
 * @param endpoint    the configuration of the target Docker Engine
 * @param error       an explanation of why the context is unavailable, or an empty string if the context is
 *                    available
 */
public record ContextElement(Context context, boolean current, String description, String endpoint,
                             String error)
{
	/**
	 * Creates a context element.
	 *
	 * @param context     the context
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
		requireThat(context, "context").isNotNull();
		requireThat(description, "description").isStripped();
		requireThat(endpoint, "endpoint").doesNotContainWhitespace().isNotEmpty();
		requireThat(error, "error").isStripped();
	}
}