package io.github.cowwoc.anchor4j.docker.client;

import io.github.cowwoc.anchor4j.core.client.Client;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmMemberException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.docker.internal.client.DefaultDocker;
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
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * A Docker client.
 */
public interface Docker extends Client
{
	/**
	 * Creates a client that uses the {@code docker} executable located in the {@code PATH} environment
	 * variable.
	 *
	 * @return a client
	 * @throws IOException if an I/O error occurs while reading file attributes
	 */
	static Docker connect() throws IOException
	{
		return new DefaultDocker();
	}

	/**
	 * Creates a client that uses the specified executable.
	 *
	 * @param executable the path of the {@code docker} executable
	 * @return a client
	 * @throws NullPointerException     if {@code executable} is null
	 * @throws IllegalArgumentException if the path referenced by {@code executable} does not exist or is not a
	 *                                  file
	 * @throws IOException              if an I/O error occurs while reading {@code executable}'s attributes
	 */
	static Docker connect(Path executable) throws IOException
	{
		return new DefaultDocker(executable);
	}

	/**
	 * Authenticates with the Docker Hub registry.
	 *
	 * @param username the user's name
	 * @param password the user's password
	 * @return this
	 * @throws NullPointerException     if any of the mandatory parameters are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>any of the arguments contain whitespace.</li>
	 *                                    <li>{@code username} or {@code password} is empty.</li>
	 *                                  </ul>
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	Docker login(String username, String password) throws IOException, InterruptedException;

	/**
	 * Authenticates with a registry.
	 *
	 * @param username      the user's name
	 * @param password      the user's password
	 * @param serverAddress the name of a registry server
	 * @return this
	 * @throws NullPointerException     if any of the mandatory parameters are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>any of the arguments contain whitespace.</li>
	 *                                    <li>any of the arguments are empty.</li>
	 *                                  </ul>
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	Docker login(String username, String password, String serverAddress)
		throws IOException, InterruptedException;

	/**
	 * Lists all the configs.
	 *
	 * @return an empty list if no match is found
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	List<ConfigElement> listConfigs() throws IOException, InterruptedException;

	/**
	 * Returns a reference to a config.
	 *
	 * @param id the config's ID or name
	 * @return null if no match is found
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 */
	Config config(String id);

	/**
	 * Looks up a config's state.
	 *
	 * @param id the config's ID or name
	 * @return null if no match is found
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	ConfigState getConfigState(String id) throws IOException, InterruptedException;

	/**
	 * Creates a config.
	 *
	 * @return a config creator
	 */
	@CheckReturnValue
	ConfigCreator createConfig();

	/**
	 * Lists all the containers.
	 *
	 * @return an empty list if no match is found
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	List<ContainerElement> listContainers() throws IOException, InterruptedException;

	/**
	 * Returns a reference to a container.
	 *
	 * @param id the container's ID or name
	 * @return the image
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace, or is empty
	 */
	Container container(String id);

	/**
	 * Looks up a container's state.
	 *
	 * @param id the ID or name
	 * @return null if no match is found
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace, or is empty
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	ContainerState getContainerState(String id) throws IOException, InterruptedException;

	/**
	 * Creates a container.
	 *
	 * @param imageId the image ID or {@link DockerImage reference} to create the container from
	 * @return a container creator
	 * @throws NullPointerException     if {@code imageId} is null
	 * @throws IllegalArgumentException if {@code imageId}:
	 *                                  <ul>
	 *                                    <li>contains whitespace or is empty.</li>
	 *                                    <li>contains uppercase letters.</li>
	 *                                  </ul>
	 */
	@CheckReturnValue
	ContainerCreator createContainer(String imageId);

	/**
	 * Renames a container.
	 *
	 * @param oldName the container's existing name
	 * @param newName the container's new name
	 * @return this
	 * @throws IllegalArgumentException  if {@code oldName} or {@code newName}:
	 *                                   <ul>
	 *                                     <li>are empty.</li>
	 *                                     <li>contain any character other than lowercase letters (a–z),
	 *                                     digits (0–9), and the following characters: {@code '.'}, {@code '/'},
	 *                                     {@code ':'}, {@code '_'}, {@code '-'}, {@code '@'}.</li>
	 *                                   </ul>
	 * @throws ResourceNotFoundException if the container does not exist
	 * @throws ResourceInUseException    if the requested name is in use by another container
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	Docker renameContainer(String oldName, String newName)
		throws IOException, InterruptedException;

	/**
	 * Starts a container.
	 *
	 * @param id the container's ID or name
	 * @return a container starter
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id}'s format is invalid
	 */
	@CheckReturnValue
	ContainerStarter startContainer(String id);

	/**
	 * Stops a container.
	 *
	 * @param id the container's ID or name
	 * @return a container stopper
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id}'s format is invalid
	 */
	@CheckReturnValue
	ContainerStopper stopContainer(String id);

	/**
	 * Removes a container.
	 *
	 * @param id the container's ID or name
	 * @return a container remover
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id}'s format is invalid
	 */
	@CheckReturnValue
	ContainerRemover removeContainer(String id);

	/**
	 * Waits until a container stops.
	 * <p>
	 * If the container has already stopped, this method returns immediately.
	 *
	 * @param id the container's ID or name
	 * @return the exit code returned by the container
	 * @throws NullPointerException      if {@code id} is null
	 * @throws IllegalArgumentException  if {@code id}'s format is invalid
	 * @throws ResourceNotFoundException if the container does not exist
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	int waitUntilContainerStops(String id) throws IOException, InterruptedException;

	/**
	 * Retrieves a container's logs.
	 *
	 * @param id the container's ID or name
	 * @return the logs
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id}'s format is invalid
	 */
	ContainerLogs getContainerLogs(String id);

	/**
	 * Lists all the contexts.
	 *
	 * @return the contexts
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	List<ContextElement> listContexts() throws IOException, InterruptedException;

	/**
	 * Returns a reference to a context.
	 *
	 * @param name the name
	 * @return the context
	 * @throws NullPointerException     if {@code name} is null
	 * @throws IllegalArgumentException if {@code name}'s format is invalid
	 */
	Context context(String name);

	/**
	 * Looks up a context's state.
	 *
	 * @param name the name
	 * @return null if no match is found
	 * @throws NullPointerException     if {@code name} is null
	 * @throws IllegalArgumentException if {@code name}'s format is invalid
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	ContextState getContextState(String name) throws IOException, InterruptedException;

	/**
	 * Creates a context.
	 *
	 * @param name     the name of the context
	 * @param endpoint the configuration of the target Docker Engine
	 * @return a context creator
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code name} contains whitespace or is empty
	 * @see ContextEndpoint#builder(URI)
	 */
	@CheckReturnValue
	ContextCreator createContext(String name, ContextEndpoint endpoint);

	/**
	 * Removes an existing context.
	 *
	 * @param name the name of the context
	 * @return the context remover
	 * @throws NullPointerException     if {@code name} is null
	 * @throws IllegalArgumentException if {@code name} contains whitespace or is empty
	 */
	@CheckReturnValue
	ContextRemover removeContext(String name);

	/**
	 * Returns the client's current context.
	 *
	 * @return an empty string if the client is using the user's context
	 * @see <a href="https://docs.docker.com/engine/security/protect-access/">Protect the Docker daemon
	 * 	socket</a>
	 * @see <a href="https://docs.docker.com/engine/manage-resources/contexts/">global --context flag</a>
	 * @see #getUserContext()
	 */
	String getClientContext();

	/**
	 * Sets the client's current context. Unlike {@link #setUserContext(String)}, this method only updates the
	 * current client's configuration and does not affect other processes or shells.
	 *
	 * @param name the name of the context, or an empty string to use the user's context
	 * @return this
	 * @throws NullPointerException     if {@code name} is null
	 * @throws IllegalArgumentException if {@code name}'s format is invalid
	 */
	Docker setClientContext(String name);

	/**
	 * Returns the current user's current context.
	 *
	 * @return the name
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 * @see <a href="https://docs.docker.com/engine/security/protect-access/">Protect the Docker daemon
	 * 	socket</a>
	 * @see <a href="https://docs.docker.com/reference/cli/docker/context/use/">docker context use</a>
	 * @see #getClientContext()
	 */
	String getUserContext() throws IOException, InterruptedException;

	/**
	 * Sets the current user's current context. Unlike {@link #setClientContext(String)}, this method updates
	 * the persistent Docker CLI configuration and affects all future Docker CLI invocations by the user across
	 * all shells.
	 *
	 * @param name the name of the context
	 * @return this
	 * @throws NullPointerException      if {@code name} is null
	 * @throws IllegalArgumentException  if {@code name}'s format is invalid
	 * @throws ResourceNotFoundException if the context does not exist
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	Docker setUserContext(String name) throws IOException, InterruptedException;

	/**
	 * Lists all the images.
	 *
	 * @return the images
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted before the operation completes. This can happen
	 *                              due to shutdown signals.
	 */
	List<ImageElement> listImages() throws IOException, InterruptedException;

	@Override
	DockerImage image(String id);

	/**
	 * Builds an image.
	 *
	 * @return an image builder
	 */
	@Override
	@CheckReturnValue
	DockerImageBuilder buildImage();

	/**
	 * Looks up an image's state.
	 *
	 * @param id the image's ID or {@link DockerImage reference}
	 * @return null if no match is found
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id}:
	 *                                  <ul>
	 *                                    <li>contains whitespace or is empty.</li>
	 *                                    <li>contains uppercase letters.</li>
	 *                                  </ul>
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	ImageState getImageState(String id) throws IOException, InterruptedException;

	/**
	 * Adds a new tag to an existing image, creating an additional reference without duplicating image data.
	 * <p>
	 * If the target reference already exists, this method has no effect.
	 *
	 * @param source the ID or existing reference of the image
	 * @param target the new reference to create
	 * @throws NullPointerException      if any of the arguments are null
	 * @throws IllegalArgumentException  if {@code source} or {@code target}'s format are invalid
	 * @throws ResourceNotFoundException if the image does not exist
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted before the operation completes. This can
	 *                                   happen due to shutdown signals.
	 */
	void tagImage(String source, String target)
		throws IOException, InterruptedException;

	/**
	 * Pulls an image from a registry.
	 *
	 * @param reference the image's reference
	 * @return an image puller
	 * @throws NullPointerException     if {@code reference} is null
	 * @throws IllegalArgumentException if {@code reference}:
	 *                                  <ul>
	 *                                    <li>contains whitespace or is empty.</li>
	 *                                    <li>contains uppercase letters.</li>
	 *                                  </ul>
	 */
	@CheckReturnValue
	ImagePuller pullImage(String reference);

	/**
	 * Pushes an image to a registry.
	 *
	 * @param reference the image's reference
	 * @return an image pusher
	 * @throws NullPointerException     if {@code reference} is null
	 * @throws IllegalArgumentException if {@code reference}:
	 *                                  <ul>
	 *                                    <li>contains whitespace or is empty.</li>
	 *                                    <li>contains uppercase letters.</li>
	 *                                  </ul>
	 */
	@CheckReturnValue
	ImagePusher pushImage(String reference) throws IOException, InterruptedException;

	/**
	 * Removes an image.
	 *
	 * @param id the image's ID or {@link DockerImage reference}
	 * @return an image remover
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id}:
	 *                                  <ul>
	 *                                    <li>contains whitespace or is empty.</li>
	 *                                    <li>contains uppercase letters.</li>
	 *                                  </ul>
	 */
	@CheckReturnValue
	ImageRemover removeImage(String id);

	/**
	 * Creates a swarm.
	 *
	 * @return a swarm creator
	 */
	@CheckReturnValue
	SwarmCreator createSwarm();

	/**
	 * Joins an existing swarm.
	 *
	 * @return a swarm joiner
	 */
	@CheckReturnValue
	SwarmJoiner joinSwarm();

	/**
	 * Leaves a swarm.
	 *
	 * @return a swarm leaver
	 */
	@CheckReturnValue
	SwarmLeaver leaveSwarm();

	/**
	 * Returns the secret value needed to join the swarm as a manager.
	 *
	 * @return the join token
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	JoinToken getManagerJoinToken() throws IOException, InterruptedException;

	/**
	 * Returns the secret value needed to join the swarm as a worker.
	 *
	 * @return the join token
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	JoinToken getWorkerJoinToken() throws IOException, InterruptedException;

	/**
	 * Looks up a network by its ID or name.
	 *
	 * @param id the ID or name
	 * @return null if no match is found
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code id} contains whitespace, or is empty
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	NetworkState getNetworkState(String id) throws IOException, InterruptedException;

	/**
	 * Lists all the nodes.
	 *
	 * @return the nodes
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	List<NodeElement> listNodes() throws IOException, InterruptedException;

	/**
	 * Lists the manager nodes in the swarm.
	 *
	 * @return the manager nodes
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	List<NodeElement> listManagerNodes() throws IOException, InterruptedException;

	/**
	 * Lists the worker nodes in the swarm.
	 *
	 * @return the worker nodes
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	List<NodeElement> listWorkerNodes() throws IOException, InterruptedException;

	/**
	 * Looks up the current node's ID.
	 *
	 * @return the ID
	 * @throws NotSwarmMemberException if the current node is not a member of a swarm
	 * @throws IOException             if an I/O error occurs. These errors are typically transient, and
	 *                                 retrying the request may resolve the issue.
	 * @throws InterruptedException    if the thread is interrupted before the operation completes. This can
	 *                                 happen due to shutdown signals.
	 */
	String getCurrentNodeId() throws IOException, InterruptedException;

	/**
	 * Returns a reference to a node.
	 *
	 * @param id the node's ID or hostname
	 * @return the node
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 */
	Node node(String id);

	/**
	 * Looks up the current node's state.
	 *
	 * @return the state
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	NodeState getNodeState() throws IOException, InterruptedException;

	/**
	 * Looks up a node's state.
	 *
	 * @param id the node's ID or hostname
	 * @return null if no match is found
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws FileNotFoundException    if the node's unix socket endpoint does not exist
	 * @throws ConnectException         if the node's TCP/IP socket refused a connection
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	NodeState getNodeState(String id) throws IOException, InterruptedException;

	/**
	 * Lists the tasks that are assigned to the current node.
	 * <p>
	 * This includes tasks in active lifecycle states such as {@code New}, {@code Allocated}, {@code Pending},
	 * {@code Assigned}, {@code Accepted}, {@code Preparing}, {@code Ready}, {@code Starting}, and
	 * {@code Running}. These states represent tasks that are in progress or actively running and are reliably
	 * returned by this command.
	 * <p>
	 * However, tasks that have reached a terminal state—such as {@code Complete}, {@code Failed}, or
	 * {@code Shutdown}— are often pruned by Docker shortly after they exit, and are therefore not guaranteed to
	 * appear in the results, even if they completed very recently.
	 * <p>
	 * Note that Docker prunes old tasks aggressively from this command, so {@link #listTasksByService(String)}
	 * will often provide more comprehensive historical data by design.
	 *
	 * @return the tasks
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	List<TaskState> listTasksByNode() throws IOException, InterruptedException;

	/**
	 * Lists the tasks that are assigned to a node.
	 * <p>
	 * Note that Docker prunes old tasks aggressively from this command, so {@link #listTasksByService(String)}
	 * will often provide more comprehensive historical data by design.
	 *
	 * @param id the node's ID or hostname
	 * @return the tasks
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	List<TaskState> listTasksByNode(String id) throws IOException, InterruptedException;

	/**
	 * Begins gracefully removing tasks from this node and redistribute them to other active nodes.
	 *
	 * @param id the node's ID or hostname
	 * @return the node's updated ID
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	String drainNode(String id) throws IOException, InterruptedException;

	/**
	 * Sets the role of a node.
	 *
	 * @param id       the node's ID or hostname
	 * @param role     the new role
	 * @param deadline the absolute time by which the type must change. The method will poll the node's state
	 *                 while the current time is before this value.
	 * @return the node's updated ID
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if a node attempts to modify its own role
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 * @throws TimeoutException         if the deadline expires before the operation succeeds
	 */
	String setNodeRole(String id, Role role, Instant deadline)
		throws IOException, InterruptedException, TimeoutException;

	/**
	 * Removes a node from the swarm.
	 *
	 * @param id the ID of the node
	 * @return an node remover
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 */
	@CheckReturnValue
	NodeRemover removeNode(String id);

	/**
	 * Returns a reference to a service.
	 *
	 * @param id the ID of the service
	 * @return the service
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 */
	Service service(String id);

	/**
	 * Lists a service's tasks.
	 *
	 * @param id the service's ID or name
	 * @return the tasks
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	List<TaskState> listTasksByService(String id) throws IOException, InterruptedException;

	/**
	 * Creates a service.
	 *
	 * @param imageId the image ID or {@link DockerImage reference} to create the service from
	 * @return a service creator
	 * @throws NullPointerException     if {@code imageId} is null
	 * @throws IllegalArgumentException if {@code imageId}'s format is invalid
	 */
	ServiceCreator createService(String imageId);

	/**
	 * Looks up a task's state.
	 *
	 * @param id the task's ID
	 * @return null if no match is found
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} contains whitespace or is empty
	 * @throws NotSwarmManagerException if the current node is not a swarm manager
	 * @throws FileNotFoundException    if the node's unix socket endpoint does not exist
	 * @throws ConnectException         if the node's TCP/IP socket refused a connection
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted before the operation completes. This can
	 *                                  happen due to shutdown signals.
	 */
	TaskState getTaskState(String id) throws IOException, InterruptedException;
}