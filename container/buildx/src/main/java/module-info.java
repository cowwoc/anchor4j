/**
 * A buildx client.
 */
module io.github.cowwoc.anchor4j.container.buildx
{
	requires io.github.cowwoc.anchor4j.core;
	requires transitive io.github.cowwoc.anchor4j.container.core;
	requires io.github.cowwoc.requirements12.annotation;
	requires io.github.cowwoc.requirements12.java;
	requires io.github.cowwoc.pouch.core;
	requires com.fasterxml.jackson.annotation;

	exports io.github.cowwoc.anchor4j.buildx.client;
	exports io.github.cowwoc.anchor4j.buildx.internal.client to io.github.cowwoc.anchor4j.container.buildx.test;
}