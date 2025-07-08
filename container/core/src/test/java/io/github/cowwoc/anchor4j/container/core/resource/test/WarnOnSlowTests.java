package io.github.cowwoc.anchor4j.container.core.resource.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Updates the number of threads used to run unit tests based on the number of processor cores and available
 * database connections.
 */
public final class WarnOnSlowTests implements ITestListener
{
	private final Map<String, ScheduledFuture<?>> testToFuture = new ConcurrentHashMap<>();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final Logger log = LoggerFactory.getLogger(WarnOnSlowTests.class);

	@Override
	public void onTestStart(ITestResult result)
	{
		String testName = getTestName(result);
		ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() ->
		{
			log.warn("{} is still running", testName);
		}, 1, 1, TimeUnit.MINUTES);
		testToFuture.put(testName, future);
	}

	private String getTestName(ITestResult result)
	{
		return result.getTestClass().getName() + "." + result.getMethod().getMethodName();
	}

	@Override
	public void onTestSuccess(ITestResult result)
	{
		onTestCompleted(result);
	}

	private void onTestCompleted(ITestResult result)
	{
		ScheduledFuture<?> future = testToFuture.remove(getTestName(result));
		future.cancel(false);
	}

	@Override
	public void onTestFailure(ITestResult result)
	{
		onTestCompleted(result);
	}

	@Override
	public void onTestSkipped(ITestResult result)
	{
		onTestCompleted(result);
	}

	@Override
	public void onTestFailedWithTimeout(ITestResult result)
	{
		onTestCompleted(result);
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result)
	{
		onTestCompleted(result);
	}
}