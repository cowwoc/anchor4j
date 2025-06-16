package io.github.cowwoc.anchor4j.docker.test.resource;

import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.anchor4j.docker.exception.AlreadySwarmMemberException;
import io.github.cowwoc.anchor4j.docker.exception.NotSwarmManagerException;
import io.github.cowwoc.anchor4j.docker.exception.ResourceInUseException;
import io.github.cowwoc.anchor4j.docker.resource.JoinToken;
import io.github.cowwoc.anchor4j.docker.resource.NodeElement;
import io.github.cowwoc.anchor4j.docker.resource.NodeState;
import io.github.cowwoc.anchor4j.docker.resource.NodeState.Role;
import io.github.cowwoc.anchor4j.docker.resource.SwarmCreator.WelcomePackage;
import io.github.cowwoc.anchor4j.docker.test.IntegrationTestContainer;
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
		manager.getClient().createSwarm().create();
		manager.onSuccess();
	}

	@Test(expectedExceptions = AlreadySwarmMemberException.class)
	public void createSwarmAlreadyInSwarm() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		manager.getClient().createSwarm().create();
		try
		{
			manager.getClient().createSwarm().create();
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
		WelcomePackage welcomePackage = manager.getClient().createSwarm().create();

		IntegrationTestContainer worker = new IntegrationTestContainer("worker");
		worker.getClient().joinSwarm().join(welcomePackage.workerJoinToken());
		manager.onSuccess();
		worker.onSuccess();
	}

	@Test(expectedExceptions = AlreadySwarmMemberException.class)
	public void joinSwarmAlreadyJoined() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		WelcomePackage welcomePackage = manager.getClient().createSwarm().create();

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

	@Test(expectedExceptions = NotSwarmManagerException.class)
	public void getNodeNotInSwarm() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer node = new IntegrationTestContainer();
		try
		{
			node.getClient().getNodeState();
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
		Docker client = manager.getClient();
		client.createSwarm().create();
		client.leaveSwarm().force().leave();
		manager.onSuccess();
	}

	@Test
	public void getCurrentNode() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager1 = new IntegrationTestContainer("manager1");
		Docker client = manager1.getClient();
		WelcomePackage welcomePackage = client.createSwarm().create();

		requireThat(client.getCurrentNodeId(), "manager1Node").isEqualTo(welcomePackage.managerId(),
			"welcomePackage.managerId()");
	}

	@Test
	public void demoteManagerBeforeLeaving() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager1 = new IntegrationTestContainer("manager1");
		WelcomePackage welcomePackage = manager1.getClient().createSwarm().create();
		JoinToken managerJoinToken = manager1.getClient().getManagerJoinToken();

		IntegrationTestContainer manager2 = new IntegrationTestContainer("manager2");
		manager2.getClient().joinSwarm().join(managerJoinToken);
		NodeState manager2NodeState = manager2.getClient().getNodeState();

		manager2.getClient().setNodeRole(welcomePackage.managerId(), Role.WORKER, Instant.now().plusSeconds(30));
		manager1.getClient().leaveSwarm().leave();
		manager2NodeState = manager2NodeState.reload();

		NodeElement nodeElement = new NodeElement(manager2.getClient().node(manager2NodeState.getId()),
			manager2NodeState.getHostname(), manager2NodeState.getRole(), manager2NodeState.isLeader(),
			manager2NodeState.getStatus(), manager2NodeState.getReachability(), manager2NodeState.getAvailability(),
			manager2NodeState.getDockerVersion());
		requireThat(manager2.getClient().listManagerNodes(), "managerNodes").
			containsExactly(List.of(nodeElement));
		manager1.onSuccess();
		manager2.onSuccess();
	}

	@Test(expectedExceptions = ResourceInUseException.class)
	public void lastManagerLeaveSwarmWithoutForce() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer manager = new IntegrationTestContainer("manager");
		manager.getClient().createSwarm().create();
		try
		{
			manager.getClient().leaveSwarm().leave();
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
		WelcomePackage welcomePackage = manager.getClient().createSwarm().create();
		JoinToken workerJoinToken = welcomePackage.workerJoinToken();
		workerJoinToken = new JoinToken(workerJoinToken.role(),
			workerJoinToken.token().substring(0, workerJoinToken.token().length() / 2),
			workerJoinToken.managerAddress());
		welcomePackage = new WelcomePackage(welcomePackage.managerId(), workerJoinToken);
		manager.getClient().leaveSwarm().force().leave();

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
		WelcomePackage welcomePackage = manager.getClient().createSwarm().create();
		manager.getClient().leaveSwarm().force().leave();

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