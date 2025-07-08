package io.github.cowwoc.anchor4j.digitalocean.project.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.client.ComputeClient;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.project.internal.client.DefaultProjectClient;
import io.github.cowwoc.anchor4j.digitalocean.project.resource.Project;

import java.io.IOException;
import java.time.Instant;

import static io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient.REST_SERVER;
import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultProject implements Project
{
	private final DefaultProjectClient client;
	private final Id id;
	private final String name;
	private final String ownerUuid;
	private final int ownerId;
	private final String description;
	private final String purpose;
	private final Environment environment;
	private final boolean isDefault;
	private final Instant createdAt;
	private final Instant updatedAt;

	/**
	 * Creates a new project.
	 *
	 * @param client      the client configuration
	 * @param id          the ID of the project
	 * @param ownerUuid   the UUID of the project owner
	 * @param ownerId     the UUID of the project id
	 * @param name        the name of the project
	 * @param description a description of the project
	 * @param purpose     the purpose of the project
	 * @param environment the project's environment
	 * @param isDefault   {@code true} if this is the default project
	 * @param createdAt   the time the project was created
	 * @param updatedAt   the time the project was last updated
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code ownerUuid}, {@code name} or {@code purpose} contain leading or
	 *                                  trailing whitespace or are empty
	 * @see ComputeClient#getDefaultVpc(Region.Id)
	 */
	public DefaultProject(DefaultProjectClient client, Id id, String ownerUuid, int ownerId, String name,
		String description, String purpose, Environment environment, boolean isDefault, Instant createdAt,
		Instant updatedAt)
	{
		requireThat(client, "client").isNotNull();
		requireThat(name, "name").isStripped().isNotEmpty();
		requireThat(ownerUuid, "ownerUuid").isStripped().isNotEmpty();
		requireThat(description, "description").isStripped();
		requireThat(purpose, "purpose").isStripped().isNotEmpty();
		requireThat(environment, "environment").isNotNull();
		requireThat(createdAt, "createdAt").isNotNull();
		requireThat(updatedAt, "updatedAt").isNotNull();

		this.client = client;
		this.id = id;
		this.name = name;
		this.ownerUuid = ownerUuid;
		this.ownerId = ownerId;
		this.description = description;
		this.purpose = purpose;
		this.environment = environment;
		this.isDefault = isDefault;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getOwnerUuid()
	{
		return ownerUuid;
	}

	@Override
	public int getOwnerId()
	{
		return ownerId;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public String getPurpose()
	{
		return purpose;
	}

	@Override
	public Environment getEnvironment()
	{
		return environment;
	}

	@Override
	public boolean isDefault()
	{
		return isDefault;
	}

	@Override
	public Instant getCreatedAt()
	{
		return createdAt;
	}

	@Override
	public Instant getUpdatedAt()
	{
		return updatedAt;
	}

	@Override
	public void destroy() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Projects/operation/projects_delete
		client.destroyResource(REST_SERVER.resolve("v2/projects/" + id));
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultProject.class).
			add("id", id).
			add("name", name).
			add("ownerUuid", ownerUuid).
			add("ownerId", ownerId).
			add("description", description).
			add("purpose", purpose).
			add("environment", environment).
			add("isDefault", isDefault).
			add("createdAt", createdAt).
			add("updatedAt", updatedAt).
			toString();
	}
}