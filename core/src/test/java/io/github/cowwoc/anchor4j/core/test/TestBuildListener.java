package io.github.cowwoc.anchor4j.core.test;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.core.resource.CommandResult;
import io.github.cowwoc.anchor4j.core.resource.DefaultBuildListener;
import io.github.cowwoc.anchor4j.core.resource.WaitFor;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A build listener that monitors which callback methods get invoked.
 */
public final class TestBuildListener extends DefaultBuildListener
{
	public final AtomicBoolean buildStarted = new AtomicBoolean();
	public final AtomicBoolean waitUntilBuildCompletes = new AtomicBoolean();
	public final AtomicBoolean buildPassed = new AtomicBoolean();
	public final AtomicBoolean buildFailed = new AtomicBoolean();
	public final AtomicBoolean buildCompleted = new AtomicBoolean();

	@Override
	public void buildStarted(BufferedReader stdoutReader, BufferedReader stderrReader, WaitFor waitFor)
	{
		buildStarted.set(true);
		waitUntilBuildCompletes.set(false);
		buildPassed.set(false);
		buildFailed.set(false);
		buildCompleted.set(false);
		super.buildStarted(stdoutReader, stderrReader, waitFor);
	}

	@Override
	public Output waitUntilBuildCompletes() throws IOException, InterruptedException
	{
		waitUntilBuildCompletes.set(true);
		return super.waitUntilBuildCompletes();
	}

	@Override
	public void buildPassed()
	{
		buildPassed.set(true);
		super.buildPassed();
	}

	@Override
	public void buildFailed(CommandResult result) throws IOException
	{
		buildFailed.set(true);
		super.buildFailed(result);
	}

	@Override
	public void buildCompleted() throws IOException
	{
		buildCompleted.set(true);
		super.buildCompleted();
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(TestBuildListener.class).
			add("buildStarted", buildStarted.get()).
			add("waitUntilBuildCompletes", waitUntilBuildCompletes.get()).
			add("buildPassed", buildPassed.get()).
			add("buildFailed", buildFailed.get()).
			add("buildCompleted", buildCompleted.get()).
			toString();
	}
}