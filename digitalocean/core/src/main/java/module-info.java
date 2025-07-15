module io.github.cowwoc.anchor4j.digitalocean.core
{
	requires transitive io.github.cowwoc.anchor4j.core;
	requires io.github.cowwoc.pouch.core;
	requires io.github.cowwoc.requirements12.java;
	requires io.github.cowwoc.requirements12.jackson;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jsr310;
	requires org.eclipse.jetty.client;
	requires org.eclipse.jetty.util;

	exports io.github.cowwoc.anchor4j.digitalocean.core.client;
	exports io.github.cowwoc.anchor4j.digitalocean.core.exception;
	exports io.github.cowwoc.anchor4j.digitalocean.core.util;

	exports io.github.cowwoc.anchor4j.digitalocean.core.internal.client to
		io.github.cowwoc.anchor4j.digitalocean.compute, io.github.cowwoc.anchor4j.digitalocean.database,
		io.github.cowwoc.anchor4j.digitalocean.registry, io.github.cowwoc.anchor4j.digitalocean.kubernetes, io.github.cowwoc.anchor4j.digitalocean.network, io.github.cowwoc.anchor4j.digitalocean.project, io.github.cowwoc.anchor4j.digitalocean.driftdetection;
}