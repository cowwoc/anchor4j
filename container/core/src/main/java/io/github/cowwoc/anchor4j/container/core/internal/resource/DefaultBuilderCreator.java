package io.github.cowwoc.anchor4j.container.core.internal.resource;

import io.github.cowwoc.anchor4j.container.core.internal.client.InternalContainerClient;
import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.container.core.resource.Builder;
import io.github.cowwoc.anchor4j.container.core.resource.Builder.Id;
import io.github.cowwoc.anchor4j.container.core.resource.BuilderCreator;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultBuilderCreator implements BuilderCreator
{
	private final InternalContainerClient client;
	private String name = "";
	private boolean startEagerly;
	private Driver driver;
	private String context = "";

	/**
	 * Creates a builder creator.
	 *
	 * @param client the client configuration
	 */
	public DefaultBuilderCreator(InternalContainerClient client)
	{
		assert client != null;
		this.client = client;
	}

	@Override
	public BuilderCreator name(String name)
	{
		ParameterValidator.validateName(name, "name");
		this.name = name;
		return this;
	}

	@Override
	public BuilderCreator startEagerly()
	{
		this.startEagerly = true;
		return this;
	}

	@Override
	public BuilderCreator driver(Driver driver)
	{
		requireThat(driver, "driver").isNotNull();
		this.driver = driver;
		return this;
	}

	@Override
	public BuilderCreator context(String context)
	{
		ParameterValidator.validateName(context, "context");
		this.context = context;
		return this;
	}

	@Override
	public Builder.Id apply() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/buildx/create/
		List<String> arguments = new ArrayList<>(7);
		arguments.add("buildx");
		arguments.add("create");
		if (!name.isEmpty())
		{
			arguments.add("--name");
			arguments.add(name);
		}
		if (driver != null)
			arguments.addAll(driver.toCommandLine());
		if (!context.isEmpty())
			arguments.add(context);
		return client.retry(_ ->
		{
			CommandResult result = client.run(arguments);
			return client.getBuildXParser().create(result);
		});
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultBuilderCreator.class).
			add("name", name).
			add("startEagerly", startEagerly).
			add("driver", driver).
			add("context", context).
			toString();
	}
}