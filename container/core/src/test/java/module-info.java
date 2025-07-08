/**
 * Common test code.
 */
module io.github.cowwoc.anchor4j.container.core.test
{
	requires transitive io.github.cowwoc.anchor4j.container.core;
	requires transitive org.testng;

	exports io.github.cowwoc.anchor4j.container.core.resource.test;
}