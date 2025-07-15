/**
 * Common code.
 */
module io.github.cowwoc.anchor4j.container.core
{
	requires transitive org.slf4j;
	requires io.github.cowwoc.requirements12.java;
	requires io.github.cowwoc.pouch.core;
	requires com.fasterxml.jackson.databind;
	requires transitive io.github.cowwoc.anchor4j.core;

	exports io.github.cowwoc.anchor4j.container.core.client;
	exports io.github.cowwoc.anchor4j.container.core.resource;
	exports io.github.cowwoc.anchor4j.container.core.exception;

	exports io.github.cowwoc.anchor4j.container.core.internal.client to
		io.github.cowwoc.anchor4j.container.buildx, io.github.cowwoc.anchor4j.container.docker,
		io.github.cowwoc.anchor4j.container.docker.test, io.github.cowwoc.anchor4j.container.buildx.test,
		io.github.cowwoc.anchor4j.container.core.test;
	exports io.github.cowwoc.anchor4j.container.core.internal.util to io.github.cowwoc.anchor4j.container.core.test,
		io.github.cowwoc.anchor4j.container.buildx, io.github.cowwoc.anchor4j.container.buildx.test,
		io.github.cowwoc.anchor4j.container.docker, io.github.cowwoc.anchor4j.container.docker.test, io.github.cowwoc.anchor4j.digitalocean.registry;
	exports io.github.cowwoc.anchor4j.container.core.internal.resource to
		io.github.cowwoc.anchor4j.container.buildx, io.github.cowwoc.anchor4j.container.buildx.test,
		io.github.cowwoc.anchor4j.container.docker, io.github.cowwoc.anchor4j.container.docker.test;
	exports io.github.cowwoc.anchor4j.container.core.internal.parser to io.github.cowwoc.anchor4j.container.buildx, io.github.cowwoc.anchor4j.container.buildx.test, io.github.cowwoc.anchor4j.container.docker, io.github.cowwoc.anchor4j.container.docker.test;
}