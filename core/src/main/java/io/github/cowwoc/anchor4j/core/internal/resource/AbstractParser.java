package io.github.cowwoc.anchor4j.core.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.core.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.github.cowwoc.requirements12.jackson.DefaultJacksonValidators.requireThat;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Code shared by all parsers.
 */
public abstract class AbstractParser
{
	protected final Client client;
	protected final Logger log = LoggerFactory.getLogger(AbstractParser.class);

	/**
	 * Creates a new AbstractParser.
	 *
	 * @param client the client configuration
	 */
	public AbstractParser(Client client)
	{
		assert client != null;
		this.client = client;
	}

	/**
	 * @return the client
	 */
	protected Client getClient()
	{
		return client;
	}

	/**
	 * Returns the {@code int} value of a JSON node.
	 *
	 * @param parent a JSON node
	 * @param name   the name of the child node
	 * @return the {@code int} value of the child node
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the child's value is not an integer or does not fit in an
	 *                                  {@code int}
	 */
	public int getInt(JsonNode parent, String name)
	{
		JsonNode child = requireThat(parent, "parent").property(name).getValue();
		requireThat(child, name).isIntegralNumber();
		requireThat(child.canConvertToInt(), name + ".canConvertToInt()").withContext(child, "child").isTrue();
		return child.intValue();
	}

	/**
	 * Returns the {@code int} representation of a JSON string node.
	 *
	 * @param parent a JSON node
	 * @param name   the name of the child node
	 * @return the {@code int} representation of the child node
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the child's value is not a String or does not fit in an {@code int}
	 */
	public int stringToInt(JsonNode parent, String name)
	{
		JsonNode child = requireThat(parent, "parent").property(name).getValue();
		requireThat(child, name).isString();
		try
		{
			return Integer.parseInt(child.textValue());
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException(name + " cannot be converted to an integer", e);
		}
	}

	/**
	 * Returns the {@code boolean} value of a JSON node.
	 * <p>
	 * If the child node does not exist, this method returns {@code false}.
	 *
	 * @param parent a JSON node
	 * @param name   the name of the child node
	 * @return the {@code boolean} value
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the child's value is not a boolean
	 */
	public boolean getBoolean(JsonNode parent, String name)
	{
		JsonNode child = parent.get(name);
		if (child == null)
			return false;
		requireThat(child, name).isBoolean();
		return child.booleanValue();
	}

	/**
	 * Returns the {@code List<T>} representation of a JSON array.
	 *
	 * @param <T>    the type of elements in the array
	 * @param parent a JSON node
	 * @param name   the name of the child node
	 * @param mapper a function that transforms the server response into a list of elements
	 * @return the {@code List<String>} value
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if the child's value is not a {@code List<String>}
	 */
	public <T> List<T> toList(JsonNode parent, String name, JsonToObject<T> mapper)
		throws IOException, InterruptedException
	{
		JsonNode arrayNode = parent.get(name);
		if (arrayNode == null)
			return List.of();
		List<T> list = new ArrayList<>(arrayNode.size());
		try
		{
			for (JsonNode element : arrayNode)
				list.add(mapper.map(element));
		}
		catch (RuntimeException e)
		{
			log.warn("Response body: {}", arrayNode.toPrettyString(), e);
			throw e;
		}
		return list;
	}

	/**
	 * Returns the elements of a JSON array.
	 *
	 * @param <E>    the type to convert the elements into
	 * @param parent a JSON node
	 * @param name   the name of the child node
	 * @param mapper a function that transforms the server response into a set of elements
	 * @return the {@code Set<E>} value
	 * @throws NullPointerException  if any of the arguments are null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	public <E> Set<E> getElements(JsonNode parent, String name, JsonToObject<E> mapper)
		throws IOException, InterruptedException
	{
		JsonNode arrayNode = parent.get(name);
		if (arrayNode == null)
			return Set.of();
		Set<E> set = HashSet.newHashSet(arrayNode.size());
		try
		{
			for (JsonNode element : arrayNode)
				set.add(mapper.map(element));
		}
		catch (RuntimeException e)
		{
			log.warn("Response body: {}", arrayNode.toPrettyString(), e);
			throw e;
		}
		return set;
	}
}