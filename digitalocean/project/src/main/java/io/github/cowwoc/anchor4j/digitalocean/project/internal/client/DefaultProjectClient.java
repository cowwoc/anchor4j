package io.github.cowwoc.anchor4j.digitalocean.project.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient;
import io.github.cowwoc.anchor4j.digitalocean.project.client.ProjectClient;
import io.github.cowwoc.anchor4j.digitalocean.project.internal.resource.ProjectParser;
import io.github.cowwoc.anchor4j.digitalocean.project.resource.Project;
import io.github.cowwoc.anchor4j.digitalocean.project.resource.Project.Id;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class DefaultProjectClient extends AbstractDigitalOceanInternalClient
	implements ProjectClient
{
	private final ProjectParser parser = new ProjectParser();

	/**
	 * Creates a new DefaultNetworkClient.
	 */
	public DefaultProjectClient()
	{
	}

	/**
	 * Returns the parser.
	 *
	 * @return the parser
	 */
	public ProjectParser getParser()
	{
		return parser;
	}

	@Override
	public Set<Project> getProjects() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Projects/operation/projects_list
		return getElements(REST_SERVER.resolve("v2/projects"), Map.of(), body ->
		{
			Set<Project> projects = new HashSet<>();
			for (JsonNode projectNode : body.get("projects"))
				projects.add(parser.projectFromServer(this, projectNode));
			return projects;
		});
	}

	@Override
	public Project getProject(Predicate<Project> predicate) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Projects/operation/projects_list
		return getElement(REST_SERVER.resolve("v2/projects"), Map.of(), body ->
		{
			for (JsonNode projectNode : body.get("projects"))
			{
				Project candidate = parser.projectFromServer(this, projectNode);
				if (predicate.test(candidate))
					return candidate;
			}
			return null;
		});
	}

	@Override
	public Project getProject(Id id) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Projects/operation/projects_get
		return getResource(REST_SERVER.resolve("v2/projects/" + id.getValue()), body ->
		{
			JsonNode project = body.get("project");
			return parser.projectFromServer(this, project);
		});
	}

	@Override
	public Project getDefaultProject() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Projects/operation/projects_get_default
		return getElement(REST_SERVER.resolve("v2/projects/default"), Map.of(), body ->
			parser.projectFromServer(this, body.get("project")));
	}
}