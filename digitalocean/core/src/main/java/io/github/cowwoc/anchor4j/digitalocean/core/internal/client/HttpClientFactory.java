package io.github.cowwoc.anchor4j.digitalocean.core.internal.client;

import io.github.cowwoc.pouch.core.ConcurrentLazyFactory;
import io.github.cowwoc.pouch.core.WrappedCheckedException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.time.Duration;

/**
 * Creates and destroys an {@code HttpClient}.
 */
public final class HttpClientFactory extends ConcurrentLazyFactory<HttpClient>
{
	private final Duration clientTimeout;
	private final QueuedThreadPool clientExecutor;

	/**
	 * Creates a new HttpClientFactory.
	 */
	public HttpClientFactory()
	{
		this.clientTimeout = Duration.ofSeconds(30);
		this.clientExecutor = new QueuedThreadPool();
		clientExecutor.setName(HttpClient.class.getSimpleName());
	}

	@Override
	protected HttpClient createValue()
	{
		HttpClient client = new HttpClient();
		client.setExecutor(clientExecutor);
		client.setConnectTimeout(clientTimeout.toMillis());
		client.setIdleTimeout(clientTimeout.toMillis());

		try
		{
			client.start();
		}
		catch (Exception e)
		{
			throw WrappedCheckedException.wrap(e);
		}
		return client;
	}

	@Override
	protected void disposeValue(HttpClient client)
	{
		try
		{
			client.stop();
		}
		catch (Exception e)
		{
			throw WrappedCheckedException.wrap(e);
		}
	}
}