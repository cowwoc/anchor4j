package io.github.cowwoc.anchor4j.docker.internal.client;

import io.github.cowwoc.anchor4j.core.internal.client.AbstractInternalClient;
import io.github.cowwoc.anchor4j.core.internal.util.ParameterValidator;
import io.github.cowwoc.anchor4j.core.internal.util.Paths;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.internal.resource.ConfigParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.ContainerParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.ContextParser;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultConfig;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultConfigCreator;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContainer;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContainerCreator;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContainerLogs;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContainerRemover;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContainerStarter;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContainerStopper;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContext;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContextCreator;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultContextRemover;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultDockerImage;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultDockerImageBuilder;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultImagePuller;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultImagePusher;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultImageRemover;
import io.github.cowwoc.anchor4j.docker.internal.resource.DefaultNode;
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
import io.github.cowwoc.anchor4j.docker.resource.ConfigState;
import io.github.cowwoc.anchor4j.docker.resource.Container;
import io.github.cowwoc.anchor4j.docker.resource.ContainerCreator;
import io.github.cowwoc.anchor4j.docker.resource.ContainerElement;
import io.github.cowwoc.anchor4j.docker.resource.ContainerLogs;
import io.github.cowwoc.anchor4j.docker.resource.ContainerRemover;
import io.github.cowwoc.anchor4j.docker.resource.ContainerStarter;
import io.github.cowwoc.anchor4j.docker.resource.ContainerState;
import io.github.cowwoc.anchor4j.docker.resource.ContainerStopper;
import io.github.cowwoc.anchor4j.docker.resource.Context;
import io.github.cowwoc.anchor4j.docker.resource.ContextCreator;
import io.github.cowwoc.anchor4j.docker.resource.ContextElement;
import io.github.cowwoc.anchor4j.docker.resource.ContextEndpoint;
import io.github.cowwoc.anchor4j.docker.resource.ContextRemover;
import io.github.cowwoc.anchor4j.docker.resource.ContextState;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.DockerImageBuilder;
import io.github.cowwoc.anchor4j.docker.resource.ImageElement;
import io.github.cowwoc.anchor4j.docker.resource.ImagePuller;
import io.github.cowwoc.anchor4j.docker.resource.ImagePusher;
import io.github.cowwoc.anchor4j.docker.resource.ImageRemover;
import io.github.cowwoc.anchor4j.docker.resource.ImageState;
import io.github.cowwoc.anchor4j.docker.resource.JoinToken;
import io.github.cowwoc.anchor4j.docker.resource.NetworkState;
import io.github.cowwoc.anchor4j.docker.resource.Node;
import io.github.cowwoc.anchor4j.docker.resource.NodeElement;
import io.github.cowwoc.anchor4j.docker.resource.NodeRemover;
import io.github.cowwoc.anchor4j.docker.resource.NodeState;
import io.github.cowwoc.anchor4j.docker.resource.NodeState.Role;
import io.github.cowwoc.anchor4j.docker.resource.Service;
import io.github.cowwoc.anchor4j.docker.resource.ServiceCreator;
import io.github.cowwoc.anchor4j.docker.resource.SwarmCreator;
import io.github.cowwoc.anchor4j.docker.resource.SwarmJoiner;
import io.github.cowwoc.anchor4j.docker.resource.SwarmLeaver;
import io.github.cowwoc.anchor4j.docker.resource.TaskState;
import io.github.cowwoc.pouch.core.ConcurrentLazyReference;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The default implementation of {@code InternalDocker}.
 */
public final class DefaultDocker extends AbstractInternalClient
	implements InternalDocker
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

	private String clientContext = "";
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
	 * @throws IOException if an I/O error occurs while reading file attributes
	 */
	public DefaultDocker() throws IOException
	{
		this(getExecutableFromPath());
	}

	/**
	 * Creates a client.
	 *
	 * @param executable the path of the Docker client
	 * @throws NullPointerException     if {@code executable} is null
	 * @throws IllegalArgumentException if the path referenced by {@code executable} does not exist or is not a
	 *                                  file
	 * @throws IOException              if an I/O error occurs while reading {@code executable}'s attributes
	 */
	public DefaultDocker(Path executable) throws IOException
	{
		super(executable);
	}

	@Override
	public ProcessBuilder getProcessBuilder(List<String> arguments)
	{
		List<String> command = new ArrayList<>(arguments.size() + 3);
		command.add(executable.toString());
		if (!clientContext.isEmpty())
		{
			command.add("--context");
			command.add(clientContext);
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
	public Docker login(String username, String password)
		throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/login/
		List<String> arguments = List.of("login", "--username", username, "--password-stdin");
		retry(deadline -> run(arguments, ByteBuffer.wrap(password.getBytes(UTF_8)), deadline));
		return this;
	}

	@Override
	public Docker login(String username, String password, String serverAddress)
		throws IOException, InterruptedException
	{
		requireThat(username, "username").doesNotContainWhitespace().isNotEmpty();
		requireThat(password, "password").doesNotContainWhitespace().isNotEmpty();
		requireThat(serverAddress, "serverAddress").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/login/
		List<String> arguments = List.of("login", "--username", username, "--password-stdin", serverAddress);
		retry(deadline -> run(arguments, ByteBuffer.wrap(password.getBytes(UTF_8)), deadline));
		return this;
	}

	@Override
	public List<ConfigElement> listConfigs() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/config/ls/
		List<String> arguments = List.of("config", "ls", "--format", "json");
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getConfigParser().list(result);
	}

	@Override
	public Config config(String id)
	{
		return new DefaultConfig(this, id);
	}

	@Override
	public ConfigState getConfigState(String id) throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/config/inspect/
		List<String> arguments = List.of("config", "inspect", id);
		CommandResult result = retry(deadline -> run(arguments, deadline));
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
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getContainerParser().list(result);
	}

	@Override
	public Container container(String id)
	{
		return new DefaultContainer(this, id);
	}

	@Override
	public ContainerState getContainerState(String id) throws IOException, InterruptedException
	{
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/container/inspect/
		List<String> arguments = List.of("container", "inspect", id);
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getContainerParser().getState(result);
	}

	@Override
	public ContainerCreator createContainer(String imageId)
	{
		return new DefaultContainerCreator(this, imageId);
	}

	@Override
	public Docker renameContainer(String oldName, String newName)
		throws IOException, InterruptedException
	{
		ParameterValidator.validateName(oldName, "oldName");
		ParameterValidator.validateName(newName, "newName");

		// https://docs.docker.com/reference/cli/docker/container/rename/
		List<String> arguments = List.of("container", "rename", oldName, newName);
		CommandResult result = retry(deadline -> run(arguments, deadline));
		getContainerParser().rename(result);
		return this;
	}

	@Override
	public ContainerStarter startContainer(String id)
	{
		return new DefaultContainerStarter(this, id);
	}

	@Override
	public ContainerStopper stopContainer(String id)
	{
		return new DefaultContainerStopper(this, id);
	}

	@Override
	public ContainerRemover removeContainer(String id)
	{
		return new DefaultContainerRemover(this, id);
	}

	@Override
	public int waitUntilContainerStops(String id) throws IOException, InterruptedException
	{
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/container/wait/
		List<String> arguments = List.of("container", "wait", id);
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getContainerParser().waitUntilStopped(result);
	}

	@Override
	public ContainerLogs getContainerLogs(String id)
	{
		return new DefaultContainerLogs(this, id);
	}

	@Override
	public List<ContextElement> listContexts() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/context/ls/
		List<String> arguments = List.of("context", "ls", "--format", "json");
		return retry(deadline ->
		{
			CommandResult result = run(arguments, deadline);
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
	public ContextState getContextState(String name) throws IOException, InterruptedException
	{
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/context/inspect/
		List<String> arguments = List.of("context", "inspect", name);
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getContextParser().getState(result);
	}

	@Override
	public Context context(String name)
	{
		return new DefaultContext(name);
	}

	@Override
	public ContextCreator createContext(String name, ContextEndpoint endpoint)
	{
		return new DefaultContextCreator(this, name, endpoint);
	}

	@Override
	public ContextRemover removeContext(String name)
	{
		return new DefaultContextRemover(this, name);
	}

	@Override
	public String getClientContext()
	{
		return clientContext;
	}

	@Override
	public Docker setClientContext(String name)
	{
		requireThat(name, "name").doesNotContainWhitespace();
		this.clientContext = name;
		return this;
	}

	@Override
	public String getUserContext() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/context/show/
		List<String> arguments = List.of("context", "show");
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getContextParser().show(result);
	}

	@Override
	public Docker setUserContext(String name) throws IOException, InterruptedException
	{
		requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/context/use/
		List<String> arguments = List.of("context", "use", name);
		CommandResult result = retry(deadline -> run(arguments, deadline));
		getContextParser().use(result);
		return this;
	}

	@Override
	public List<ImageElement> listImages() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/image/ls/
		List<String> arguments = List.of("image", "ls", "--format", "json", "--all", "--digests", "--no-trunc");
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getImageParser().list(result);
	}

	@Override
	public DockerImage image(String id)
	{
		return new DefaultDockerImage(this, id);
	}

	@Override
	public ImageState getImageState(String id) throws IOException, InterruptedException
	{
		ParameterValidator.validateImageReference(id, "id");

		// https://docs.docker.com/reference/cli/docker/image/inspect/
		List<String> arguments = List.of("image", "inspect", "--format", "json", id);
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getImageParser().getState(result);
	}

	@Override
	public void tagImage(String source, String target) throws IOException, InterruptedException
	{
		ParameterValidator.validateImageIdOrReference(source, "source");
		ParameterValidator.validateImageReference(target, "target");

		// https://docs.docker.com/reference/cli/docker/image/tag/
		List<String> arguments = List.of("image", "tag", source, target);
		CommandResult result = retry(deadline -> run(arguments, deadline));
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
	public ImageRemover removeImage(String id)
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
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getSwarmParser().getJoinToken(result, Role.MANAGER);
	}

	@Override
	public JoinToken getWorkerJoinToken() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/swarm/join-token/
		List<String> arguments = List.of("swarm", "join-token", "worker");
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getSwarmParser().getJoinToken(result, Role.WORKER);
	}

	@Override
	public NetworkState getNetworkState(String id) throws IOException, InterruptedException
	{
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/network/inspect/
		List<String> arguments = List.of("network", "inspect", id);
		CommandResult result = retry(deadline -> run(arguments, deadline));
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
		CommandResult result = retry(deadline -> run(arguments, deadline));
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
	public Node node(String id)
	{
		return new DefaultNode(this, id);
	}

	@Override
	public String getCurrentNodeId() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/system/info/
		List<String> arguments = new ArrayList<>(4);
		arguments.add("system");
		arguments.add("info");
		arguments.add("--format");
		arguments.add("{{json .Swarm.NodeID}}");
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getNodeParser().getNodeId(result);
	}

	@Override
	public NodeState getNodeState() throws IOException, InterruptedException
	{
		return getNodeState("self");
	}

	@Override
	public NodeState getNodeState(String id) throws IOException, InterruptedException
	{
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/node/inspect/
		List<String> arguments = List.of("node", "inspect", id);
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getNodeParser().getState(result);
	}

	@Override
	public List<TaskState> listTasksByNode() throws IOException, InterruptedException
	{
		// https://docs.docker.com/reference/cli/docker/node/ps/
		List<String> arguments = new ArrayList<>(5);
		arguments.add("node");
		arguments.add("ps");
		arguments.add("--format");
		arguments.add("json");
		arguments.add("--no-trunc");
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getNodeParser().listTasksByNode(result);
	}

	@Override
	public List<TaskState> listTasksByNode(String id) throws IOException, InterruptedException
	{
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/node/ps/
		List<String> arguments = new ArrayList<>(6);
		arguments.add("node");
		arguments.add("ps");
		arguments.add("--format");
		arguments.add("json");
		arguments.add("--no-trunc");
		arguments.add(id);
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getNodeParser().listTasksByNode(result);
	}

	@Override
	public String setNodeRole(String id, Role role, Instant deadline)
		throws IOException, InterruptedException, TimeoutException
	{
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();

		String currentId = getCurrentNodeId();
		if (id.equals(currentId))
		{
			// Although a manager node can technically modify its own role, there is no way of monitoring the
			// change. To ensure observation of the operation, we disallow self-modification and require another
			// manager to perform it.
			throw new IllegalArgumentException("A node cannot modify its own role");
		}

		// https://docs.docker.com/reference/cli/docker/node/update/
		List<String> arguments = List.of("node", "update", "--role=" + role.name().toLowerCase(Locale.ROOT), id);
		CommandResult result = retry(deadline2 -> run(arguments, deadline2));
		String newId = getNodeParser().setRole(result);
		retry(deadline2 ->
		{
			NodeState state = getNodeState(newId);
			while (state.getRole() != role)
			{
				if (!sleepBeforeRetry(deadline2))
					throw new TimeoutException();
				state = getNodeState(newId);
			}
			return null;
		}, deadline);
		return newId;
	}

	@Override
	public String drainNode(String id) throws IOException, InterruptedException
	{
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/node/update/
		List<String> arguments = List.of("node", "update", "--availability=drain", id);
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getNodeParser().setRole(result);
	}

	@Override
	public NodeRemover removeNode(String id)
	{
		return new DefaultNodeRemover(this, id);
	}

	@Override
	public Service service(String id)
	{
		return new DefaultService(this, id);
	}

	@Override
	public ServiceCreator createService(String imageId)
	{
		return new DefaultServiceCreator(this, imageId);
	}

	@Override
	public List<TaskState> listTasksByService(String id) throws IOException, InterruptedException
	{
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/service/ps/
		List<String> arguments = new ArrayList<>(5);
		arguments.add("service");
		arguments.add("ps");
		arguments.add("--format");
		arguments.add("json");
		arguments.add(id);
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getNodeParser().listTasksByService(result);
	}

	@Override
	public TaskState getTaskState(String id) throws IOException, InterruptedException
	{
		requireThat(id, "id").doesNotContainWhitespace().isNotEmpty();

		// https://docs.docker.com/reference/cli/docker/inspect/
		List<String> arguments = new ArrayList<>(4);
		arguments.add("inspect");
		arguments.add("--type");
		arguments.add("task");
		arguments.add(id);
		CommandResult result = retry(deadline -> run(arguments, deadline));
		return getNodeParser().getTaskState(result);
	}
}