package io.github.cowwoc.anchor4j.core.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Parses a JSON document into a Java object.
 *
 * @param <T> the type of value returned by this mapper
 */
public interface JsonToObject<T>
{
	/**
	 * Parses JSON into an Object.
	 *
	 * @param json the JSON document
	 * @return the Java object
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted while waiting for a response. This can happen
	 *                              due to shutdown signals.
	 */
	T map(JsonNode json) throws IOException, InterruptedException;
}