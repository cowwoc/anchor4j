package io.github.cowwoc.anchor4j.docker.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.TestNG;

import java.nio.file.Path;
import java.util.List;

/**
 * Runs integration tests continuously until a failure is encountered.
 */
public final class SmokeTest
{
	public static void main(String[] args)
	{
		Logger log = LoggerFactory.getLogger(SmokeTest.class);

		int iteration = 1;
		while (true)
		{
			log.info("🔁 Iteration #{}", iteration);
			TestNG testng = new TestNG();
			testng.setTestSuites(List.of(Path.of("target/test-classes/testng.xml").toString()));
			testng.setDefaultSuiteName("LoopSuite");
			testng.setDefaultTestName("LoopTest");
			testng.setOutputDirectory("target/test-output");
			testng.addListener(new FailureLogger());
			testng.run();

			if (testng.hasFailure() || testng.hasSkip())
			{
				log.error("❌ Failure or skip detected on iteration {}", iteration);
				System.exit(1);
			}
			++iteration;
		}
	}

	/**
	 * Logs any test failures.
	 */
	public static final class FailureLogger implements ITestListener
	{
		private final Logger log = LoggerFactory.getLogger(FailureLogger.class);

		@Override
		public void onTestFailure(ITestResult result)
		{
			log.warn("❌ Test failed: {}.{}", result.getTestClass().getName(), result.getMethod().getMethodName(),
				result.getThrowable());
		}
	}
}