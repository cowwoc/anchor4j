package io.github.cowwoc.anchor4j.digitalocean.project.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cowwoc.anchor4j.digitalocean.core.internal.client.AbstractDigitalOceanInternalClient;
import io.github.cowwoc.anchor4j.digitalocean.project.client.ProjectClient;
import io.github.cowwoc.anchor4j.digitalocean.project.internal.parser.ProjectParser;
import io.github.cowwoc.anchor4j.digitalocean.project.resource.Project;
import io.github.cowwoc.anchor4j.digitalocean.project.resource.Project.Id;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DefaultProjectClient extends AbstractDigitalOceanInternalClient
	implements ProjectClient
{
	private final ProjectParser parser = new ProjectParser(this);

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
	public List<Project> getProjects() throws IOException, InterruptedException
	{
		return getProjects(_ -> true);
	}

	@Override
	public List<Project> getProjects(Predicate<Project> predicate) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Projects/operation/projects_list
		return getElements(REST_SERVER.resolve("v2/projects"), Map.of(), body ->
		{
			List<Project> projects = new ArrayList<>();
			for (JsonNode projectNode : body.get("projects"))
			{
				Project candidate = parser.projectFromServer(projectNode);
				if (predicate.test(candidate))
					projects.add(candidate);
			}
			return projects;
		});
	}

	@Override
	public Project getProject(Id id) throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Projects/operation/projects_get
		return getResource(REST_SERVER.resolve("v2/projects/" + id.getValue()), body ->
		{
			JsonNode project = body.get("project");
			return parser.projectFromServer(project);
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
				Project candidate = parser.projectFromServer(projectNode);
				if (predicate.test(candidate))
					return candidate;
			}
			return null;
		});
	}

	@Override
	public Project getDefaultProject() throws IOException, InterruptedException
	{
		// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Projects/operation/projects_get_default
		return getElement(REST_SERVER.resolve("v2/projects/default"), Map.of(), body ->
			parser.projectFromServer(body.get("project")));
	}

	@Override
	public List<Object> getResources(Predicate<? super Class<?>> typeFilter, Predicate<Object> resourceFilter)
		throws IOException, InterruptedException
	{
		ensureOpen();
		Set<Class<?>> types = Set.of(Project.class).stream().filter(typeFilter).collect(Collectors.toSet());
		if (types.isEmpty())
			return List.of();
		return List.copyOf(getProjects(resourceFilter::test));
	}
}