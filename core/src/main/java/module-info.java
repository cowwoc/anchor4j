/**
 * Common code.
 */
module io.github.cowwoc.anchor4j.core
{
	requires transitive org.slf4j;
	requires io.github.cowwoc.requirements12.java;
	requires io.github.cowwoc.pouch.core;
	requires com.fasterxml.jackson.databind;

	exports io.github.cowwoc.anchor4j.core.client;
	exports io.github.cowwoc.anchor4j.core.resource;
	exports io.github.cowwoc.anchor4j.core.exception;

	exports io.github.cowwoc.anchor4j.core.internal.client to
		io.github.cowwoc.anchor4j.buildx, io.github.cowwoc.anchor4j.docker,
		io.github.cowwoc.anchor4j.docker.test, io.github.cowwoc.anchor4j.buildx.test,
		io.github.cowwoc.anchor4j.core.test;
	exports io.github.cowwoc.anchor4j.core.internal.resource to
		io.github.cowwoc.anchor4j.buildx, io.github.cowwoc.anchor4j.docker,
		io.github.cowwoc.anchor4j.docker.test, io.github.cowwoc.anchor4j.buildx.test;
	exports io.github.cowwoc.anchor4j.core.internal.util to io.github.cowwoc.anchor4j.core.test,
		io.github.cowwoc.anchor4j.buildx, io.github.cowwoc.anchor4j.buildx.test,
		io.github.cowwoc.anchor4j.docker, io.github.cowwoc.anchor4j.docker.test;
}