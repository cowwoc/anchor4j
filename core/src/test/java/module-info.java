/**
 * Common test code.
 */
module io.github.cowwoc.anchor4j.core.test
{
	requires io.github.cowwoc.anchor4j.core;

	exports io.github.cowwoc.anchor4j.core.test to
		io.github.cowwoc.anchor4j.buildx.test, io.github.cowwoc.anchor4j.docker.test;
}