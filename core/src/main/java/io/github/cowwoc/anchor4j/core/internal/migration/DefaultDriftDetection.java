package io.github.cowwoc.anchor4j.core.internal.migration;

import io.github.cowwoc.anchor4j.core.client.Client;
import io.github.cowwoc.anchor4j.core.migration.DriftDetection;
import io.github.cowwoc.requirements12.java.ValidationFailure;
import io.github.cowwoc.requirements12.java.ValidationFailures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.checkIf;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultDriftDetection implements DriftDetection
{
	private List<Client> clients = new ArrayList<>();
	private Predicate<? super Class<?>> typeFilter = _ -> true;
	private Predicate<Object> resourceFilter = _ -> true;
	private boolean closed;
	private final Logger log = LoggerFactory.getLogger(DefaultDriftDetection.class);

	/**
	 * Registers a client to run drift detection against.
	 *
	 * @param client the client
	 */
	public void addClient(Client client)
	{
		assert client != null;
		clients.add(client);
	}

	@Override
	public DriftDetection includeResourceType(Predicate<? super Class<?>> resourceTypeFilter)
	{
		requireThat(resourceTypeFilter, "resourceTypeFilter").isNotNull();
		this.typeFilter = resourceTypeFilter;
		return this;
	}

	@Override
	public DriftDetection includeResource(Predicate<Object> resourceFilter)
	{
		requireThat(resourceFilter, "resourceFilter").isNotNull();
		this.resourceFilter = resourceFilter;
		return this;
	}

	@Override
	public void report() throws IOException, InterruptedException
	{
		ensureOpen();
		List<Object> expected = new ArrayList<>();
		for (Client client : clients)
			expected.addAll(client.getResources(typeFilter, resourceFilter));

		List<Object> actual = new ArrayList<>();
		for (Client client : clients)
			actual.addAll(client.getResources(typeFilter, resourceFilter));

		ValidationFailures failures = checkIf(actual, "actual").containsExactly(expected, "expected").
			elseGetFailures();
		for (ValidationFailure failure : failures.getFailures())
			log.error(failure.getMessage());
	}

	/**
	 * Ensures that the client is open.
	 *
	 * @throws IllegalStateException if the client is closed
	 */
	private void ensureOpen()
	{
		if (isClosed())
			throw new IllegalStateException("client was closed");
	}

	@Override
	public boolean isClosed()
	{
		return closed;
	}

	@Override
	public void close()
	{
		closed = true;
	}
}