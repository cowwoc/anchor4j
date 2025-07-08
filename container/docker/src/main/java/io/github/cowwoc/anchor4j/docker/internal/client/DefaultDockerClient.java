package io.github.cowwoc.anchor4j.docker.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.cowwoc.anchor4j.container.core.internal.client.AbstractInternalContainer;
import io.github.cowwoc.anchor4j.container.core.internal.client.CommandRunner;
import io.github.cowwoc.anchor4j.container.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage;
import io.github.cowwoc.anchor4j.core.internal.util.Paths;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.docker.internal.resource.ConfigParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.ContainerParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.ContextParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultConfigCreator;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContainerCreator;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContainerLogs;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContainerRemover;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContainerStarter;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContainerStopper;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContextCreator;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContextRemover;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultDockerImageBuilder;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultImagePuller;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultImagePusher;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultImageRemover;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultNodeRemover;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultService;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultServiceCreator;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultSwarmCreator;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultSwarmJoiner;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultSwarmLeaver;
import io.github.cowwoc.anchor4j.docker.internal.resource.ImageParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.NetworkParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.NodeParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.ServiceParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.SwarmParser;
import io.github.cowwoc.anchor4j.docker.resource.Config;
import io.github.cowwoc.anchor4j.docker.resource.ConfigCreator;
import io.github.cowwoc.anchor4j.docker.resource.ConfigElement;
import io.github.cowwoc.anchor4j.docker.resource.Container;
import io.github.cowwoc.anchor4j.docker.resource.ContainerCreator;
import io.github.cowwoc.anchor4j.docker.resource.ContainerElement;
import io.github.cowwoc.anchor4j.docker.resource.ContainerLogs;
import io.github.cowwoc.anchor4j.docker.resource.ContainerRemover;
import io.github.cowwoc.anchor4j.docker.resource.ContainerStarter;
import io.github.cowwoc.anchor4j.docker.resource.ContainerStopper;
import io.github.cowwoc.anchor4j.docker.resource.Context;
import io.github.cowwoc.anchor4j.docker.resource.ContextCreator;
import io.github.cowwoc.anchor4j.docker.resource.ContextElement;
import io.github.cowwoc.anchor4j.docker.resource.ContextEndpoint;
import io.github.cowwoc.anchor4j.docker.resource.ContextRemover;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.DockerImageBuilder;
import io.github.cowwoc.anchor4j.docker.resource.ImageElement;
import io.github.cowwoc.anchor4j.docker.resource.ImagePuller;
import io.github.cowwoc.anchor4j.docker.resource.ImagePusher;
import io.github.cowwoc.anchor4j.docker.resource.ImageRemover;
import io.github.cowwoc.anchor4j.docker.resource.JoinToken;
import io.github.cowwoc.anchor4j.docker.resource.Network;
import io.github.cowwoc.anchor4j.docker.resource.Node;
import io.github.cowwoc.anchor4j.docker.resource.Node.Id;
import io.github.cowwoc.anchor4j.docker.resource.Node.Role;
import io.github.cowwoc.anchor4j.docker.resource.NodeElement;
import io.github.cowwoc.anchor4j.docker.resource.NodeRemover;
import io.github.cowwoc.anchor4j.docker.resource.Service;
import io.github.cowwoc.anchor4j.docker.resource.ServiceCreator;
import io.github.cowwoc.anchor4j.docker.resource.SwarmCreator;
import io.github.cowwoc.anchor4j.docker.resource.SwarmJoiner;
import io.github.cowwoc.anchor4j.docker.resource.SwarmLeaver;
import io.github.cowwoc.anchor4j.docker.resource.Task;
import io.github.cowwoc.pouch.core.ConcurrentLazyReference;
import io.github.cowwoc.pouch.core.WrappedCheckedException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings("PMD.MoreThanOneLogger")
public final class DefaultDockerClient extends AbstractInternalContainer
	implements InternalDockerClient
{
	private static final ConcurrentLazyReference<Path> EXECUTABLE_FROM_PATH = ConcurrentLazyReference.create(
		() ->
		{
			Path path = Paths.searchPath(List.of("docker"));
			if (path == null)
				throw new UncheckedIOException(new IOException("Could not find docker on the PATH"));
			return path;
		});

	/**
	 * @return the path of the {@code docker} executable located in the {@code PATH} environment variable
	 */
	private static Path getExecutableFromPath() throws IOException
	{
		try
		{
			return EXECUTABLE_FROM_PATH.getValue();
		}
		catch (UncheckedIOException e)
		{
			throw e.getCause();
		}
	}

	/**
	 * The exit code returned by Docker in response to SIGTERM.
	 */
	private static final int SIGTERM = 143;
	private Context.Id clientContext;
	@SuppressWarnings("this-escape")
	private final ConfigParser configParser = new ConfigParser(this);
	@SuppressWarnings("this-escape")
	private final ContainerParser containerParser = new ContainerParser(this);
	@SuppressWarnings("this-escape")
	private final ImageParser imageParser = new ImageParser(this);
	@SuppressWarnings("this-escape")
	private final ContextParser contextParser = new ContextParser(this);
	@SuppressWarnings("this-escape")
	private final NetworkParser networkParser = new NetworkParser(this);
	@SuppressWarnings("this-escape")
	private final NodeParser nodeParser = new NodeParser(this);
	@SuppressWarnings("this-escape")
	private final ServiceParser serviceParser = new ServiceParser(this);
	@SuppressWarnings("this-escape")
	private final SwarmParser swarmParser = new SwarmParser(this);

	/**
	 * Creates a client that uses the {@code docker} executable located in the {@code PATH} environment
	 * variable.
	 *
	 * @throws IOException if an I/O error occurs while building the client
	 */
	public DefaultDockerClient() throws IOException
	{
		this(getExecutableFromPath());
	}

	/**
	 * Returns a client.
	 *
	 * @param executable the path of the Docker client
	 * @throws NullPointerException     if {@code executable} is null
	 * @throws IllegalArgumentException if the path referenced by {@code executable} does not exist or is not a
	 *                                  file
	 * @throws IOException              if an I/O error occurs while reading {@code executable}'s attributes
	 */
	private DefaultDockerClient(Path executable) throws IOException
	{
		super(executable);
	}

	@Override
	public DockerClient retryTimeout(Duration duration)
	{
		return (DockerClient) super.retryTimeout(duration);
	}

	@Override
	public ProcessBuilder getProcessBuilder(List<String> arguments)
	{
		List<String> command = new ArrayList<>(arguments.size() + 3);
		command.add(executable.toString());
		if (clientContext != null)
		{
			command.add("--context");
			command.add(clientContext.getValue());
		}
		command.addAll(arguments);
		return new ProcessBuilder(command);
	}

	@Override
	public DockerImageBuilder buildImage()
	{
		return new DefaultDockerImageBuilder(this);
	}

	@Override
	public ConfigParser getConfigParser()
	{
		return configParser;
	}

	@Override
	public ContainerParser getContainerParser()
	{
		return containerParser;
	}

	@Override
	public ImageParser getImageParser()
	{
		return imageParser;
	}

	@Override
	public ContextParser getContextParser()
	{
		return contextParser;
	}

	@Override
	public NetworkParser getNetworkParser()
	{
		return networkParser;
	}

	@Override
	public NodeParser getNodeParser()
	{
		return nodeParser;
	}

	@Override
	public ServiceParser getServiceParser()
	{
		return serviceParser;
	}

	@Override
	public SwarmParser getSwarmParser()
	{
		return swarmParser;
	}

	@Override
	public DockerClient login(String username, String password)
		throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/login/
		List<String> arguments = List.of("login", "--username", username, "--password-stdin");
		retry(_ -> run(arguments, ByteBuffer.wrap(password.getBytes(UTF_8))));
		return this;
	}

	@Override
	public DockerClient login(String username, String password, String serverAddress)
		throws IOException, InterruptedException
	{
		requireThat(username, "username").doesNotContainWhitespace().isNotEmpty();
		requireThat(password, "password").doesNotContainWhitespace().isNotEmpty();
		requireThat(serverAddress, "serverAddress").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/login/
		List<String> arguments = List.of("login", "--username", username, "--password-stdin", serverAddress);
		retry(_ -> run(arguments, ByteBuffer.wrap(password.getBytes(UTF_8))));
		return this;
	}

	@Override
	public List<ConfigElement> listConfigs() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/config/ls/
		List<String> arguments = List.of("config", "ls", "--format", "json");
		CommandResult result = retry(_ -> run(arguments));
		return getConfigParser().list(result);
	}

	@Override
	public Config getConfig(String id) throws IOException, InterruptedException
	{
		return getConfig(Config.id(id));
	}

	@Override
	public Config getConfig(Config.Id id) throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/config/inspect/
		List<String> arguments = List.of("config", "inspect", id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		return getConfigParser().getState(result);
	}

	@Override
	public ConfigCreator createConfig()
	{
		return new DefaultConfigCreator(this);
	}

	@Override
	public List<ContainerElement> listContainers() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/container/ls/
		List<String> arguments = List.of("container", "ls", "--format", "json", "--all", "--no-trunc");
		CommandResult result = retry(_ -> run(arguments));
		return getContainerParser().list(result);
	}

	@Override
	public Container getContainer(String id) throws IOException, InterruptedException
	{
		return getContainer(Container.id(id));
	}

	@Override
	public Container getContainer(Container.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.docker.com/reference/cli/docker/container/inspect/
		List<String> arguments = List.of("container", "inspect", id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		return getContainerParser().getState(result);
	}

	@Override
	public ContainerCreator createContainer(String imageId)
	{
		return createContainer(ContainerImage.id(imageId));
	}

	@Override
	public ContainerCreator createContainer(ContainerImage.Id imageId)
	{
		return new DefaultContainerCreator(this, imageId);
	}

	@Override
	public DockerClient renameContainer(String id, String newName)
		throws IOException, InterruptedException
	{
		return renameContainer(Container.id(id), newName);
	}

	@Override
	public DockerClient renameContainer(Container.Id id, String newName)
		throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();
		ParameterValidator.validateName(newName, "newName");

		// https://docs.docker.com/reference/cli/docker/container/rename/
		List<String> arguments = List.of("container", "rename", id.getValue(), newName);
		CommandResult result = retry(_ -> run(arguments));
		getContainerParser().rename(result);
		return this;
	}

	@Override
	public ContainerStarter startContainer(String id)
	{
		return startContainer(Container.id(id));
	}

	@Override
	public ContainerStarter startContainer(Container.Id id)
	{
		return new DefaultContainerStarter(this, id);
	}

	@Override
	public ContainerStopper stopContainer(String id)
	{
		return stopContainer(Container.id(id));
	}

	@Override
	public ContainerStopper stopContainer(Container.Id id)
	{
		return new DefaultContainerStopper(this, id);
	}

	@Override
	public ContainerRemover removeContainer(String id)
	{
		return removeContainer(Container.id(id));
	}

	@Override
	public ContainerRemover removeContainer(Container.Id id)
	{
		return new DefaultContainerRemover(this, id);
	}

	@Override
	public int waitUntilContainerStops(String id) throws IOException, InterruptedException
	{
		return waitUntilContainerStops(Container.id(id));
	}

	@Override
	public int waitUntilContainerStops(Container.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.docker.com/reference/cli/docker/container/wait/
		List<String> arguments = List.of("container", "wait", id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		return getContainerParser().waitUntilStopped(result);
	}

	@Override
	public void waitUntilContainerStatus(Container.Status status, String id)
		throws IOException, InterruptedException
	{
		waitUntilContainerStatus(status, Container.id(id));
	}

	@Override
	public Container waitUntilContainerStatus(Container.Status status, Container.Id id)
		throws IOException, InterruptedException
	{
		requireThat(status, "status").isNotNull();
		requireThat(id, "id").isNotNull();

		Container container = getContainer(id);
		if (container == null)
			throw new ResourceNotFoundException("Container " + id);
		if (container.getStatus().equals(status))
			return container;
		switch (status)
		{
			case CREATED -> waitUntilContainerEvent(container, status, "create");
			case RUNNING -> waitUntilContainerEvent(container, status, "start");
			case PAUSED -> waitUntilContainerEvent(container, status, "pause");
			case RESTARTING -> waitUntilContainerEvents(Set.of("die", "start"), new Predicate<>()
			{
				private final List<String> pending = new ArrayList<>(List.of("die", "start"));

				@Override
				public boolean test(String line)
				{
					try
					{
						JsonNode json = getJsonMapper().readTree(line);
						String id = json.get("id").textValue();
						if (!container.getId().getValue().equals(id))
							return false;
						String event = json.get("status").textValue();
						if (event.equals(pending.getFirst()))
							pending.removeFirst();
						return pending.isEmpty() || container.reload().getStatus().equals(status);
					}
					catch (IOException | InterruptedException e)
					{
						throw WrappedCheckedException.wrap(e);
					}
				}
			});
			case EXITED -> waitUntilContainerEvent(container, status, "die");
			case REMOVING -> waitUntilContainerEvent(container, status, "destroy");
			case DEAD -> waitUntilContainerEvents(container, status, Set.of("die", "oom"));
		}
		return container.reload();
	}

	/**
	 * Waits for container events.
	 *
	 * @param container the container to monitor
	 * @param status    the status to wait for
	 * @param event     the event to wait for
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	private void waitUntilContainerEvent(Container container, Container.Status status, String event)
		throws IOException, InterruptedException
	{
		waitUntilContainerEvents(container, status, Set.of(event));
	}

	/**
	 * Waits for container events.
	 *
	 * @param container the container to monitor
	 * @param status    the status to wait for
	 * @param events    the events to wait for
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	private void waitUntilContainerEvents(Container container, Container.Status status, Set<String> events)
		throws IOException, InterruptedException
	{
		JsonMapper jsonMapper = getJsonMapper();
		waitUntilContainerEvents(events, line ->
		{
			try
			{
				JsonNode json = jsonMapper.readTree(line);
				String id = json.get("id").textValue();
				if (!container.getId().getValue().equals(id))
					return false;
				String event = json.get("status").textValue();
				return events.contains(event) || container.reload().getStatus().equals(status);
			}
			catch (IOException | InterruptedException e)
			{
				throw WrappedCheckedException.wrap(e);
			}
		});
	}

	/**
	 * Waits for container events.
	 *
	 * @param events            the events to wait for
	 * @param terminateOnStdout a function that consumes stdout lines and returns {@code true} if the process
	 *                          should be terminated
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	private void waitUntilContainerEvents(Set<String> events, Predicate<String> terminateOnStdout)
		throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/system/events/
		List<String> arguments = new ArrayList<>(5 + events.size());
		Collections.addAll(arguments, "system", "events", "--filter");
		for (String event : events)
			arguments.add("event=" + event);
		Collections.addAll(arguments, "--format", "json");

		CommandRunner commandRunner = new CommandRunner(getProcessBuilder(arguments)).
			terminateOnStdout(terminateOnStdout).
			failureHandler(result ->
			{
				if (result.exitCode() == SIGTERM)
					return;
				commandFailed(result);
			});
		retry(_ -> commandRunner.apply());
	}

	@Override
	public ContainerLogs getContainerLogs(String id)
	{
		return getContainerLogs(Container.id(id));
	}

	@Override
	public ContainerLogs getContainerLogs(Container.Id id)
	{
		return new DefaultContainerLogs(this, id);
	}

	@Override
	public List<ContextElement> listContexts() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/context/ls/
		List<String> arguments = List.of("context", "ls", "--format", "json");
		return retry(_ ->
		{
			CommandResult result = run(arguments);
			try
			{
				return getContextParser().list(result);
			}
			catch (IllegalArgumentException e)
			{
				// Sometimes the context "endpoint" and "error" are both empty strings. The error gets populated
				// a bit later.
				throw new IOException(e);
			}
		});
	}

	@Override
	public Context getContext(String id) throws IOException, InterruptedException
	{
		return getContext(Context.id(id));
	}

	@Override
	public Context getContext(Context.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.docker.com/reference/cli/docker/context/inspect/
		List<String> arguments = List.of("context", "inspect", id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		return getContextParser().getContext(result);
	}

	@Override
	public ContextCreator createContext(String name, ContextEndpoint endpoint)
	{
		return new DefaultContextCreator(this, name, endpoint);
	}

	@Override
	public ContextRemover removeContext(String name)
	{
		return removeContext(Context.id(name));
	}

	@Override
	public ContextRemover removeContext(Context.Id id)
	{
		return new DefaultContextRemover(this, id);
	}

	@Override
	public Context.Id getClientContext()
	{
		return clientContext;
	}

	@Override
	public DockerClient setClientContext(String id)
	{
		return setClientContext(Context.id(id));
	}

	@Override
	public DockerClient setClientContext(Context.Id id)
	{
		this.clientContext = id;
		return this;
	}

	@Override
	public Context.Id getUserContext() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/context/show/
		List<String> arguments = List.of("context", "show");
		CommandResult result = retry(_ -> run(arguments));
		return getContextParser().show(result);
	}

	@Override
	public DockerClient setUserContext(String id) throws IOException, InterruptedException
	{
		return setUserContext(Context.id(id));
	}

	@Override
	public DockerClient setUserContext(Context.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.docker.com/reference/cli/docker/context/use/
		List<String> arguments = List.of("context", "use", id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		getContextParser().use(result);
		return this;
	}

	@Override
	public List<ImageElement> listImages() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/image/ls/
		List<String> arguments = List.of("image", "ls", "--format", "json", "--all", "--digests", "--no-trunc");
		CommandResult result = retry(_ -> run(arguments));
		return getImageParser().list(result);
	}

	@Override
	public DockerImage getImage(String id) throws IOException, InterruptedException
	{
		return (DockerImage) super.getImage(id);
	}

	@Override
	public DockerImage getImage(ContainerImage.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.docker.com/reference/cli/docker/image/inspect/
		List<String> arguments = List.of("image", "inspect", "--format", "json", id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		return getImageParser().getState(result);
	}

	@Override
	public void tagImage(String id, String target) throws IOException, InterruptedException
	{
		tagImage(ContainerImage.id(id), target);
	}

	@Override
	public void tagImage(ContainerImage.Id id, String target) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();
		ParameterValidator.validateImageReference(target, "target");

		// https://docs.docker.com/reference/cli/docker/image/tag/
		List<String> arguments = List.of("image", "tag", id.getValue(), target);
		CommandResult result = retry(_ -> run(arguments));
		getImageParser().tag(result);
	}

	@Override
	public ImagePuller pullImage(String reference)
	{
		return new DefaultImagePuller(this, reference);
	}

	@Override
	public ImagePusher pushImage(String reference)
	{
		return new DefaultImagePusher(this, reference);
	}

	@Override
	public ImageRemover removeImageTag(String reference)
	{
		return new DefaultImageRemover(this, ContainerImage.id(reference));
	}

	@Override
	public ImageRemover removeImage(String id)
	{
		return removeImage(ContainerImage.id(id));
	}

	@Override
	public ImageRemover removeImage(ContainerImage.Id id)
	{
		return new DefaultImageRemover(this, id);
	}

	@Override
	public SwarmCreator createSwarm()
	{
		return new DefaultSwarmCreator(this);
	}

	@Override
	public SwarmJoiner joinSwarm()
	{
		return new DefaultSwarmJoiner(this);
	}

	@Override
	public SwarmLeaver leaveSwarm()
	{
		return new DefaultSwarmLeaver(this);
	}

	@Override
	public JoinToken getManagerJoinToken() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/swarm/join-token/
		List<String> arguments = List.of("swarm", "join-token", "manager");
		CommandResult result = retry(_ -> run(arguments));
		return getSwarmParser().getJoinToken(result, Role.MANAGER);
	}

	@Override
	public JoinToken getWorkerJoinToken() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/swarm/join-token/
		List<String> arguments = List.of("swarm", "join-token", "worker");
		CommandResult result = retry(_ -> run(arguments));
		return getSwarmParser().getJoinToken(result, Role.WORKER);
	}

	@Override
	public Network getNetwork(String id) throws IOException, InterruptedException
	{
		return getNetwork(Network.id(id));
	}

	@Override
	public Network getNetwork(Network.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.docker.com/reference/cli/docker/network/inspect/
		List<String> arguments = List.of("network", "inspect", id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		return getNetworkParser().getState(result);
	}

	@Override
	public List<NodeElement> listNodes() throws IOException, InterruptedException
	{
		return listNodes(List.of());
	}

	/**
	 * Returns nodes that match the specified filters.
	 *
	 * @param filters the filters to apply to the list
	 * @return the matching nodes
	 * @throws NullPointerException     if {@code filters} is null
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	private List<NodeElement> listNodes(List<String> filters) throws IOException, InterruptedException
	{
		assert filters != null;

		// https://docs.docker.com/reference/cli/docker/node/ls/
		List<String> arguments = new ArrayList<>(4 + filters.size() * 2);
		arguments.add("node");
		arguments.add("ls");
		arguments.add("--format");
		arguments.add("json");
		for (String filter : filters)
		{
			arguments.add("--filter");
			arguments.add(filter);
		}
		CommandResult result = retry(_ -> run(arguments));
		return getNodeParser().listNodes(result);
	}

	@Override
	public List<NodeElement> listManagerNodes() throws IOException, InterruptedException
	{
		return listNodes(List.of("role=manager"));
	}

	@Override
	public List<NodeElement> listWorkerNodes() throws IOException, InterruptedException
	{
		return listNodes(List.of("role=worker"));
	}

	@Override
	public Node.Id getCurrentNodeId() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/system/info/
		List<String> arguments = new ArrayList<>(4);
		arguments.add("system");
		arguments.add("info");
		arguments.add("--format");
		arguments.add("{{json .Swarm.NodeID}}");
		CommandResult result = retry(_ -> run(arguments));
		return getNodeParser().getNodeId(result);
	}

	@Override
	public Node getCurrentNode() throws IOException, InterruptedException
	{
		return getNode(getCurrentNodeId());
	}

	@Override
	public Node getNode(String id) throws IOException, InterruptedException
	{
		return getNode(Node.id(id));
	}

	@Override
	public Node getNode(Node.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.docker.com/reference/cli/docker/node/inspect/
		List<String> arguments = List.of("node", "inspect", id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		return getNodeParser().getNode(result);
	}

	@Override
	public List<Task> listTasksByNode() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/node/ps/
		List<String> arguments = new ArrayList<>(5);
		arguments.add("node");
		arguments.add("ps");
		arguments.add("--format");
		arguments.add("json");
		arguments.add("--no-trunc");
		CommandResult result = retry(_ -> run(arguments));
		return getNodeParser().listTasksByNode(result);
	}

	@Override
	public List<Task> listTasksByNode(String id) throws IOException, InterruptedException
	{
		return listTasksByNode(Node.id(id));
	}

	@Override
	public List<Task> listTasksByNode(Node.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.docker.com/reference/cli/docker/node/ps/
		List<String> arguments = new ArrayList<>(6);
		arguments.add("node");
		arguments.add("ps");
		arguments.add("--format");
		arguments.add("json");
		arguments.add("--no-trunc");
		arguments.add(id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		return getNodeParser().listTasksByNode(result);
	}

	@Override
	public Id setNodeRole(String id, Role role, Instant deadline)
		throws IOException, InterruptedException, TimeoutException
	{
		return setNodeRole(Node.id(id), role, deadline);
	}

	@Override
	public Node.Id setNodeRole(Node.Id id, Role role, Instant deadline)
		throws IOException, InterruptedException, TimeoutException
	{
		requireThat(id, "id").isNotNull();

		Node.Id currentId = getCurrentNodeId();
		if (id.equals(currentId))
		{
			// Although a manager node can technically modify its own role, there is no way of monitoring the
			// change. To ensure observation of the operation, we disallow self-modification and require another
			// manager to perform it.
			throw new IllegalArgumentException("A node cannot modify its own role");
		}

		// https://docs.docker.com/reference/cli/docker/node/update/
		List<String> arguments = List.of("node", "update", "--role=" + role.name().toLowerCase(Locale.ROOT),
			id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		Node.Id newId = getNodeParser().setRole(result);
		retry(deadline2 ->
		{
			Node node = getNode(newId);
			while (node.getRole() != role)
			{
				if (!sleepBeforeRetry(deadline2))
					throw new TimeoutException();
				node = getNode(newId);
			}
			return null;
		}, deadline);
		return newId;
	}

	@Override
	public Id drainNode(String id) throws IOException, InterruptedException
	{
		return drainNode(Node.id(id));
	}

	@Override
	public Node.Id drainNode(Node.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.docker.com/reference/cli/docker/node/update/
		List<String> arguments = List.of("node", "update", "--availability=drain", id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		return getNodeParser().setRole(result);
	}

	@Override
	public NodeRemover removeNode(String id)
	{
		return removeNode(Node.id(id));
	}

	@Override
	public NodeRemover removeNode(Node.Id id)
	{
		return new DefaultNodeRemover(this, id);
	}

	@Override
	public Service getService(String id)
	{
		return getService(Service.id(id));
	}

	@Override
	public Service getService(Service.Id id)
	{
		return new DefaultService(this, id);
	}

	@Override
	public ServiceCreator createService(String imageId)
	{
		return createService(ContainerImage.id(imageId));
	}

	@Override
	public ServiceCreator createService(ContainerImage.Id imageId)
	{
		return new DefaultServiceCreator(this, imageId);
	}

	@Override
	public List<Task> listTasksByService(String id) throws IOException, InterruptedException
	{
		return listTasksByService(Service.id(id));
	}

	@Override
	public List<Task> listTasksByService(Service.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.docker.com/reference/cli/docker/service/ps/
		List<String> arguments = new ArrayList<>(5);
		arguments.add("service");
		arguments.add("ps");
		arguments.add("--format");
		arguments.add("json");
		arguments.add(id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		return getNodeParser().listTasksByService(result);
	}

	@Override
	public Task getTask(String id) throws IOException, InterruptedException
	{
		return getTask(Task.id(id));
	}

	@Override
	public Task getTask(Task.Id id) throws IOException, InterruptedException
	{
		requireThat(id, "id").isNotNull();

		// https://docs.docker.com/reference/cli/docker/inspect/
		List<String> arguments = new ArrayList<>(4);
		arguments.add("inspect");
		arguments.add("--type");
		arguments.add("task");
		arguments.add(id.getValue());
		CommandResult result = retry(_ -> run(arguments));
		return getNodeParser().getTaskState(result);
	}
}