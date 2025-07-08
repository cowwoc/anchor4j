package io.github.cowwoc.anchor4j.container.docker.test.resource;

import io.github.cowwoc.anchor4j.container.docker.test.IntegrationTestContainer;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.exception.AlreadySwarmMemberException;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmMemberException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.resource.JoinToken;
import io.github.cowwoc.anchor4j.docker.resource.Node;
import io.github.cowwoc.anchor4j.docker.resource.Node.Role;
import io.github.cowwoc.anchor4j.docker.resource.NodeElement;
import io.github.cowwoc.anchor4j.docker.resource.SwarmCreator.WelcomePackage;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class SwarmIT
{
	@Test
	public void createSwarm() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		manager.getClient().createSwarm().apply();
		manager.onSuccess();
	}

	@Test(expectedExceptions = AlreadySwarmMemberException.class)
	public void createSwarmAlreadyInSwarm() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		manager.getClient().createSwarm().apply();
		try
		{
			manager.getClient().createSwarm().apply();
		}
		catch (AlreadySwarmMemberException e)
		{
			manager.onSuccess();
			throw e;
		}
	}

	@Test
	public void joinSwarm() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		WelcomePackage welcomePackage = manager.getClient().createSwarm().apply();

		IntegrationTestContainer worker = new IntegrationTestContainer("worker");
		worker.getClient().joinSwarm().join(welcomePackage.workerJoinToken());
		manager.onSuccess();
		worker.onSuccess();
	}

	@Test(expectedExceptions = AlreadySwarmMemberException.class)
	public void joinSwarmAlreadyJoined() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		WelcomePackage welcomePackage = manager.getClient().createSwarm().apply();

		IntegrationTestContainer worker = new IntegrationTestContainer("worker");
		worker.getClient().joinSwarm().join(welcomePackage.workerJoinToken());
		try
		{
			worker.getClient().joinSwarm().join(welcomePackage.workerJoinToken());
		}
		catch (AlreadySwarmMemberException e)
		{
			manager.onSuccess();
			worker.onSuccess();
			throw e;
		}
	}

	@Test(expectedExceptions = NotSwarmMemberException.class)
	public void getCurrentNodeIdNotInSwarm() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer node = new IntegrationTestContainer();
		try
		{
			node.getClient().getCurrentNodeId();
		}
		catch (NotSwarmManagerException e)
		{
			node.onSuccess();
			throw e;
		}
	}

	@Test
	public void lastManagerLeaveSwarm() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		DockerClient client = manager.getClient();
		client.createSwarm().apply();
		client.leaveSwarm().force().apply();
		manager.onSuccess();
	}

	@Test
	public void getCurrentNode() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager1 = new IntegrationTestContainer("manager1");
		DockerClient client = manager1.getClient();
		WelcomePackage welcomePackage = client.createSwarm().apply();

		requireThat(client.getCurrentNodeId(), "manager1Node").isEqualTo(welcomePackage.managerId(),
			"welcomePackage.managerId()");
	}

	@Test
	public void removeNode() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		WelcomePackage welcomePackage = manager.getClient().createSwarm().apply();
		JoinToken workerJoinToken = welcomePackage.workerJoinToken();

		IntegrationTestContainer worker = new IntegrationTestContainer("worker");
		worker.getClient().joinSwarm().join(workerJoinToken);
		Node.Id workerId = worker.getClient().getCurrentNodeId();
		Node workerNodeState = manager.getClient().getNode(workerId);

		Node workerNode = manager.getClient().getNode(workerNodeState.getId());
		workerNode.remove().force().apply();

		Node managerNodeState = manager.getClient().getCurrentNode();
		NodeElement nodeElement = new NodeElement(managerNodeState.getId(), managerNodeState.getHostname(),
			managerNodeState.getRole(), managerNodeState.isLeader(), managerNodeState.getStatus(),
			managerNodeState.getReachability(), managerNodeState.getAvailability(),
			managerNodeState.getDockerVersion());
		requireThat(manager.getClient().listNodes(), "nodes").containsExactly(List.of(nodeElement));
		manager.onSuccess();
		worker.onSuccess();
	}

	@Test
	public void alreadyRemovedNode() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		WelcomePackage welcomePackage = manager.getClient().createSwarm().apply();
		JoinToken workerJoinToken = welcomePackage.workerJoinToken();

		IntegrationTestContainer worker = new IntegrationTestContainer("worker");
		worker.getClient().joinSwarm().join(workerJoinToken);
		Node workerNodeState = manager.getClient().getNode(worker.getClient().getCurrentNodeId());

		Node workerNode = manager.getClient().getNode(workerNodeState.getId());
		workerNode.remove().force().apply();
		workerNode.remove().apply();

		Node managerNodeState = manager.getClient().getCurrentNode();
		NodeElement nodeElement = new NodeElement(managerNodeState.getId(), managerNodeState.getHostname(),
			managerNodeState.getRole(), managerNodeState.isLeader(), managerNodeState.getStatus(),
			managerNodeState.getReachability(), managerNodeState.getAvailability(),
			managerNodeState.getDockerVersion());
		requireThat(manager.getClient().listNodes(), "nodes").containsExactly(List.of(nodeElement));
		manager.onSuccess();
		worker.onSuccess();
	}

	@Test
	public void demoteManagerBeforeLeaving() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager1 = new IntegrationTestContainer("manager1");
		WelcomePackage welcomePackage = manager1.getClient().createSwarm().apply();
		JoinToken managerJoinToken = manager1.getClient().getManagerJoinToken();

		IntegrationTestContainer manager2 = new IntegrationTestContainer("manager2");
		manager2.getClient().joinSwarm().join(managerJoinToken);
		Node manager2Node = manager2.getClient().getCurrentNode();

		manager2.getClient().setNodeRole(welcomePackage.managerId(), Role.WORKER, Instant.now().plusSeconds(30));
		manager1.getClient().leaveSwarm().apply();
		manager2Node = manager2Node.reload();

		NodeElement nodeElement = new NodeElement(manager2Node.getId(), manager2Node.getHostname(),
			manager2Node.getRole(), manager2Node.isLeader(), manager2Node.getStatus(),
			manager2Node.getReachability(), manager2Node.getAvailability(), manager2Node.getDockerVersion());
		requireThat(manager2.getClient().listManagerNodes(), "managerNodes").
			containsExactly(List.of(nodeElement));
		manager1.onSuccess();
		manager2.onSuccess();
	}

	@Test(expectedExceptions = ResourceInUseException.class)
	public void lastManagerLeaveSwarmWithoutForce() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		manager.getClient().createSwarm().apply();
		try
		{
			manager.getClient().leaveSwarm().apply();
		}
		catch (ResourceInUseException e)
		{
			manager.onSuccess();
			throw e;
		}
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void joinSwarmInvalidToken() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		WelcomePackage welcomePackage = manager.getClient().createSwarm().apply();
		JoinToken workerJoinToken = welcomePackage.workerJoinToken();
		workerJoinToken = new JoinToken(workerJoinToken.role(),
			workerJoinToken.token().substring(0, workerJoinToken.token().length() / 2),
			workerJoinToken.managerAddress());
		welcomePackage = new WelcomePackage(welcomePackage.managerId(), workerJoinToken);
		manager.getClient().leaveSwarm().force().apply();

		IntegrationTestContainer worker = new IntegrationTestContainer("worker");
		try
		{
			worker.getClient().joinSwarm().join(welcomePackage.workerJoinToken());
		}
		catch (IllegalArgumentException e)
		{
			manager.onSuccess();
			worker.onSuccess();
			throw e;
		}
	}

	@Test(expectedExceptions = ConnectException.class)
	public void joinSwarmManagerIsDown() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		WelcomePackage welcomePackage = manager.getClient().createSwarm().apply();
		manager.getClient().leaveSwarm().force().apply();

		IntegrationTestContainer worker = new IntegrationTestContainer("worker");
		try
		{
			worker.getClient().joinSwarm().join(welcomePackage.workerJoinToken());
		}
		catch (ConnectException e)
		{
			manager.onSuccess();
			worker.onSuccess();
			throw e;
		}
	}
}