module io.github.cowwoc.anchor4j.digitalocean.network
{
	requires transitive io.github.cowwoc.anchor4j.digitalocean.core;
	requires io.github.cowwoc.anchor4j.core;
	requires io.github.cowwoc.pouch.core;
	requires io.github.cowwoc.requirements12.java;
	requires io.github.cowwoc.requirements12.jackson;
	requires org.eclipse.jetty.client;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jsr310;

	exports io.github.cowwoc.anchor4j.digitalocean.network.client;
	exports io.github.cowwoc.anchor4j.digitalocean.network.resource;

	exports io.github.cowwoc.anchor4j.digitalocean.network.internal.resource to
		io.github.cowwoc.anchor4j.digitalocean.compute, io.github.cowwoc.anchor4j.digitalocean.kubernetes;
}