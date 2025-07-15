/**
 * A Docker client.
 */
module io.github.cowwoc.anchor4j.container.docker
{
	requires io.github.cowwoc.anchor4j.core;
	requires transitive io.github.cowwoc.anchor4j.container.core;
	requires io.github.cowwoc.pouch.core;
	requires io.github.cowwoc.requirements12.jackson;
	requires com.fasterxml.jackson.databind;
	requires org.slf4j;

	exports io.github.cowwoc.anchor4j.docker.client;
	exports io.github.cowwoc.anchor4j.docker.exception;
	exports io.github.cowwoc.anchor4j.docker.resource;

	exports io.github.cowwoc.anchor4j.docker.internal.util to io.github.cowwoc.anchor4j.container.docker.test;
	exports io.github.cowwoc.anchor4j.docker.internal.client to io.github.cowwoc.anchor4j.container.docker.test;
	exports io.github.cowwoc.anchor4j.docker.internal.resource to
		io.github.cowwoc.anchor4j.container.docker.test;
	exports io.github.cowwoc.anchor4j.docker.internal.parser to io.github.cowwoc.anchor4j.container.docker.test;
}