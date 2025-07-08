package io.github.cowwoc.anchor4j.docker.resource;

import io.github.cowwoc.anchor4j.docker.exception.AlreadySwarmMemberException;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Joins an existing Swarm.
 */
public interface SwarmJoiner
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
	SwarmJoiner advertiseAddress(InetSocketAddress advertiseAddress);

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
	SwarmJoiner advertiseAddress(String advertiseAddress);

	/**
	 * Sets the address that the node uses to listen for inter-container communication (e.g., {@code 0.0.0.0}).
	 * <p>
	 * The port is {@link SwarmCreator#dataPathAddress(InetSocketAddress) configured} at swarm creation time and
	 * cannot be changed.
	 *
	 * @param dataPathAddress the address
	 * @return this
	 * @throws NullPointerException if {@code dataPathAddress} is null
	 */
	SwarmJoiner dataPathAddress(InetAddress dataPathAddress);

	/**
	 * Sets the address that the current node will use to listen for inter-manager communication. The default
	 * value is {@code 0.0.0.0:2377}).
	 *
	 * @param listenAddress the address
	 * @return this
	 * @throws NullPointerException if {@code listenAddress} is null
	 */
	SwarmJoiner listenAddress(InetSocketAddress listenAddress);

	/**
	 * Joins an existing swarm.
	 *
	 * @param joinToken the secret value needed to join the swarm
	 * @throws NullPointerException        if {@code joinToken} is null
	 * @throws IllegalArgumentException    if the join token is invalid
	 * @throws AlreadySwarmMemberException if the server is already a member of a swarm
	 * @throws FileNotFoundException       if a referenced unix socket was not found
	 * @throws ConnectException            if a referenced TCP/IP socket refused a connection
	 * @throws IOException                 if an I/O error occurs. These errors are typically transient, and
	 *                                     retrying the request may resolve the issue.
	 * @throws InterruptedException        if the thread is interrupted before the operation completes. This can
	 *                                     happen due to shutdown signals.
	 */
	@CheckReturnValue
	void join(JoinToken joinToken) throws IOException, InterruptedException;
}