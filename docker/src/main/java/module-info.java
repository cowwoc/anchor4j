/**
 * A Docker client.
 */
module io.github.cowwoc.anchor4j.docker
{
	requires transitive io.github.cowwoc.anchor4j.core;
	requires io.github.cowwoc.pouch.core;
	requires io.github.cowwoc.requirements12.jackson;
	requires com.fasterxml.jackson.databind;
	requires org.slf4j;

	exports io.github.cowwoc.anchor4j.docker.client;
	exports io.github.cowwoc.anchor4j.docker.exception;
	exports io.github.cowwoc.anchor4j.docker.resource;
	// Needed by unit tests
	exports io.github.cowwoc.anchor4j.docker.internal.util to io.github.cowwoc.anchor4j.docker.test;
	exports io.github.cowwoc.anchor4j.docker.internal.client to io.github.cowwoc.anchor4j.docker.test;
	exports io.github.cowwoc.anchor4j.docker.internal.resource to io.github.cowwoc.anchor4j.docker.test;
}