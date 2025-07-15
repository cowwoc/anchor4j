package io.github.cowwoc.anchor4j.digitalocean.project.internal.parser;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.core.client.Client;
import io.github.cowwoc.anchor4j.core.internal.resource.AbstractParser;
import io.github.cowwoc.anchor4j.digitalocean.project.internal.client.DefaultProjectClient;
import io.github.cowwoc.anchor4j.digitalocean.project.internal.resource.DefaultProject;
import io.github.cowwoc.anchor4j.digitalocean.project.resource.Project;
import io.github.cowwoc.anchor4j.digitalocean.project.resource.Project.Environment;
import io.github.cowwoc.anchor4j.digitalocean.project.resource.Project.Id;

import java.time.Instant;
import java.util.Locale;

/**
 * Parses server responses.
 */
public final class ProjectParser extends AbstractParser
{
	/**
	 * Creates a new ProjectParser.
	 *
	 * @param client the client configuration
	 */
	public ProjectParser(Client client)
	{
		super(client);
	}

	@Override
	protected DefaultProjectClient getClient()
	{
		return (DefaultProjectClient) super.getClient();
	}

	/**
	 * Convert a Project from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the project
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if no match is found
	 */
	public Project projectFromServer(JsonNode json)
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Projects/operation/projects_get
		Id id = Project.id(json.get("id").textValue());
		String ownerUuid = json.get("owner_uuid").textValue();
		int ownerId = getInt(json, "owner_id");
		String name = json.get("name").textValue();
		String description = json.get("description").textValue();
		String purpose = json.get("purpose").textValue();
		Environment environment = environmentFromServer(json.get("environment"));
		boolean isDefault = getBoolean(json, "is_default");
		Instant createdAt = Instant.parse(json.get("created_at").textValue());
		Instant updatedAt = Instant.parse(json.get("updated_at").textValue());
		return new DefaultProject(getClient(), id, ownerUuid, ownerId, name, description, purpose, environment,
			isDefault, createdAt, updatedAt);
	}

	/**
	 * Convert an Environment from its server representation.
	 *
	 * @param json the JSON representation
	 * @return the environment
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if no match is found
	 */
	private Environment environmentFromServer(JsonNode json)
	{
		return Environment.valueOf(json.textValue().toUpperCase(Locale.ROOT));
	}
}