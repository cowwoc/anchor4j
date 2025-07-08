package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.AlreadySwarmMemberException;

import java.io.IOException;
import java.net.InetSocketAddress;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Creates a Swarm.
 */
public interface SwarmCreator
{
	/**
	 * Sets the externally reachable address that will be advertised to other members of the swarm for API
	 * access and overlay networking. If unspecified, Docker will check if the system has a single IP address,
	 * and use that IP address with the {@link #listenAddress listening port}. If the system has multiple IP
	 * addresses, {@code advertiseAddress} must be specified so that the correct address is chosen for
	 * inter-manager communication and overlay networking.
	 * <p>
	 * If the specified port is {@code 0}, the default port {@code 2377} will be used instead.
	 *
	 * @param advertiseAddress the address
	 * @return this
	 * @throws NullPointerException if {@code advertiseAddress} is null
	 */
	SwarmCreator advertiseAddress(InetSocketAddress advertiseAddress);

	/**
	 * Sets the externally reachable address that will be advertised to other members of the swarm for API
	 * access and overlay networking. If unspecified, Docker will check if the system has a single IP address,
	 * and use that IP address with the {@link #listenAddress listening port}. If the system has multiple IP
	 * addresses, {@code advertiseAddress} must be specified so that the correct address is chosen for
	 * inter-manager communication and overlay networking.
	 * <p>
	 * It is also possible to specify a network interface to advertise that interface's address; for example
	 * {@code eth0:2377}.
	 * <p>
	 * Specifying a port is optional. If the value is a bare IP address or interface name, the default port
	 * {@code 2377} is used.
	 *
	 * @param advertiseAddress the address
	 * @return this
	 * @throws NullPointerException     if {@code advertiseAddress} is null
	 * @throws IllegalArgumentException if {@code advertiseAddress} contains whitespace, or is empty
	 */
	SwarmCreator advertiseAddress(String advertiseAddress);

	/**
	 * Sets the address that the node uses to listen for inter-container communication (e.g.,
	 * {@code 0.0.0.0:4789}).
	 * <p>
	 * If the specified port is {@code 0}, the default port {@code 4789} will be used instead; otherwise, the
	 * specified port must be within the range {@code 1024} to {@code 49151}, inclusive.
	 *
	 * @param dataPathAddress the address
	 * @return this
	 * @throws NullPointerException     if {@code dataPathAddress} is null
	 * @throws IllegalArgumentException if {@code dataPathAddress}'s port number is not zero and is less than
	 *                                  {@code 1024}, or is greater than {@code 49151}
	 */
	SwarmCreator dataPathAddress(InetSocketAddress dataPathAddress);

	/**
	 * Adds a subnet in CIDR notation to the address pool used for allocating overlay network subnets. If
	 * omitted, internal defaults are used.
	 * <p>
	 * When you initialize a Docker Swarm, Docker automatically assigns overlay networks to services. These
	 * subnets are carved from the default address pools specified here.
	 *
	 * @param defaultAddressPool the address
	 * @return this
	 * @throws NullPointerException     if {@code defaultAddressPool} is null
	 * @throws IllegalArgumentException if {@code defaultAddressPool}:
	 *                                  <ul>
	 *                                    <li>contains whitespace or is empty.</li>
	 *                                    <li>is not a valid CIDR notation.</li>
	 *                                  </ul>
	 */
	SwarmCreator defaultAddressPool(String defaultAddressPool);

	/**
	 * Sets the CIDR prefix length for each subnet allocated from the default address pool. The default value is
	 * {@code 24}.
	 * <p>
	 * When you initialize a Docker Swarm, Docker automatically assigns subnets for overlay networks. This value
	 * controls the size of each allocated subnet. Smaller values result in larger subnets, while larger values
	 * produce smaller subnets.
	 *
	 * @param subnetSize the prefix length (e.g., {@code 24} for a {@code /24} subnet)
	 * @return this
	 * @throws IllegalArgumentException if {@code subnetSize} is less than {@code 16} or greater than
	 *                                  {@code 28}
	 */
	SwarmCreator subnetSize(int subnetSize);

	/**
	 * Sets the address that the current node will use to listen for inter-manager communication. The default
	 * value is {@code 0.0.0.0:2377}).
	 *
	 * @param listenAddress the address
	 * @return this
	 * @throws NullPointerException if {@code listenAddress} is null
	 */
	SwarmCreator listenAddress(InetSocketAddress listenAddress);

	/**
	 * Creates a Swarm cluster.
	 * <p>
	 * This method sets up the current node as a Swarm manager, creating the foundation for a distributed
	 * cluster. It configures critical networking and operational parameters required for the Swarm's
	 * functionality. The cluster can subsequently be scaled by adding worker or manager nodes.
	 * </p>
	 * <p>
	 * Networking details:
	 * <ul>
	 *   <li>{@link #listenAddress Listen Address}: The address where the current node listens for
	 *   inter-manager communication, such as leader election and cluster state updates.</li>
	 *   <li>{@link #dataPathAddress Data Path Address}: The address where the current node handles
	 *   inter-container communication over Swarm overlay networks.</li>
	 *   <li>{@link #advertiseAddress Advertise Address}: The externally reachable address that other nodes
	 *   use for connecting to the manager API and joining the Swarm.</li>
	 * </ul>
	 *
	 * @return the node's ID and the worker's join token
	 * @throws AlreadySwarmMemberException if this node is already a member of a swarm
	 * @throws IOException                 if an I/O error occurs. These errors are typically transient, and
	 *                                     retrying the request may resolve the issue.
	 * @throws InterruptedException        if the thread is interrupted before the operation completes. This can
	 *                                     happen due to shutdown signals.
	 */
	WelcomePackage apply() throws IOException, InterruptedException;

	/**
	 * The information provided on successfully creating and joining a new swarm.
	 *
	 * @param managerId       the ID of the manager that created the swarm
	 * @param workerJoinToken the secret value needed to join the swarm as a worker
	 */
	record WelcomePackage(Node.Id managerId, JoinToken workerJoinToken)
	{
		/**
		 * @param managerId       the ID of the manager that created the swarm
		 * @param workerJoinToken the secret value needed to join the swarm as a worker
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code nodeId} contains whitespace or is empty
		 */
		public WelcomePackage
		{
			requireThat(managerId, "managerId").isNotNull();
			requireThat(workerJoinToken, "workerJoinToken").isNotNull();
		}
	}
}