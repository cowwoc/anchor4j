package io.github.cowwoc.anchor4j.digitalocean.core.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.core.internal.client.InternalClient;
import io.github.cowwoc.anchor4j.core.internal.resource.JsonToObject;
import io.github.cowwoc.anchor4j.digitalocean.core.client.DigitalOceanClient;
import io.github.cowwoc.anchor4j.digitalocean.core.exception.TooManyRequestsException;
import io.github.cowwoc.pouch.core.WrappedCheckedException;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * The internals of a {@code DigitalOceanClient}.
 */
public interface DigitalOceanInternalClient extends DigitalOceanClient, InternalClient
{
	/**
	 * Defines the frequency at which it is acceptable to log the same message to indicate that the thread is
	 * still active. This helps in monitoring the progress and ensuring the thread has not become unresponsive.
	 */
	Duration PROGRESS_FREQUENCY = Duration.ofSeconds(2);

	/**
	 * Creates a request without a body and sets the {@code Authorization} header.
	 *
	 * @param uri the URI the send a request to
	 * @return the request
	 * @throws NullPointerException  if {@code uri} is null
	 * @throws IllegalStateException if the client is closed
	 */
	Request createRequest(URI uri);

	/**
	 * Creates a request with a body and sets the {@code Authorization} header.
	 *
	 * @param uri  the URI the send a request to
	 * @param body the request body
	 * @return the request
	 * @throws NullPointerException  if {@code uri} is null
	 * @throws IllegalStateException if the client is closed
	 */
	Request createRequest(URI uri, JsonNode body);

	/**
	 * Returns the JSON representation of the server response.
	 *
	 * @param serverResponse the server response
	 * @return the JSON representation of the response
	 * @throws NullPointerException    if {@code serverRespoonse} is null
	 * @throws IllegalStateException   if the client is closed
	 * @throws WrappedCheckedException if the server response could not be parsed
	 */
	JsonNode getResponseBody(ContentResponse serverResponse);

	/**
	 * Returns all the elements from a paginated list.
	 *
	 * @param <T>        the type of elements in the Set
	 * @param uri        the URI to send a request to
	 * @param parameters the parameters to add to the request
	 * @param mapper     a function that transforms the server response into a set of elements
	 * @return the elements
	 * @throws NullPointerException  if any of the arguments are null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	<T> Set<T> getElements(URI uri, Map<String, Collection<String>> parameters, JsonToObject<Set<T>> mapper)
		throws IOException, InterruptedException;

	/**
	 * Returns an element from a paginated list.
	 *
	 * @param <T>        the type of elements in the list
	 * @param uri        the URI to send a request to
	 * @param parameters the parameters to add to the request
	 * @param mapper     a function that transforms the server response into a non-null element if a match is
	 *                   found
	 * @return null if no match is found
	 * @throws NullPointerException  if any of the arguments are null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	<T> T getElement(URI uri, Map<String, Collection<String>> parameters, JsonToObject<T> mapper)
		throws IOException, InterruptedException;

	/**
	 * Looks up a resource.
	 *
	 * @param <T>    the type of the resource
	 * @param uri    the URI of the resource
	 * @param mapper a function that transforms the server response into a resource if a match is found
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted while waiting for a response. This can happen
	 *                              due to shutdown signals.
	 */
	<T> T getResource(URI uri, JsonToObject<T> mapper) throws IOException, InterruptedException;

	/**
	 * Destroys a resource.
	 *
	 * @param uri the URI of the resource
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted while waiting for a response. This can happen
	 *                              due to shutdown signals.
	 */
	void destroyResource(URI uri) throws IOException, InterruptedException;

	/**
	 * Sends a request.
	 *
	 * @param request the client request
	 * @return the server response
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted while waiting for a response. This can happen
	 *                              due to shutdown signals.
	 */
	Response send(Request request) throws IOException, InterruptedException;

	/**
	 * Returns the String representation of the response
	 *
	 * @param response the server response
	 * @return the String representation
	 */
	String toString(Response response);

	/**
	 * Returns the String representation of the request.
	 *
	 * @param request a client request
	 * @return the String representation
	 */
	String toString(Request request);

	/**
	 * Converts an HTTP 429 ("Too Many Requests") server response to an exception.
	 *
	 * @param response the server response
	 * @return the exception
	 */
	TooManyRequestsException getTooManyRequestsException(Response response);
}