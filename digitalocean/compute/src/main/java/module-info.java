module io.github.cowwoc.anchor4j.digitalocean.compute
{
	requires transitive io.github.cowwoc.anchor4j.digitalocean.network;
	requires io.github.cowwoc.anchor4j.core;
	requires io.github.cowwoc.pouch.core;
	requires io.github.cowwoc.requirements12.java;
	requires io.github.cowwoc.requirements12.jackson;
	requires org.eclipse.jetty.client;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jsr310;
	requires io.github.cowwoc.anchor4j.digitalocean.core;
	requires org.apache.sshd.osgi;

	exports io.github.cowwoc.anchor4j.digitalocean.compute.client;
	exports io.github.cowwoc.anchor4j.digitalocean.compute.resource;

	exports io.github.cowwoc.anchor4j.digitalocean.compute.internal.client to
		io.github.cowwoc.anchor4j.digitalocean.database;
	exports io.github.cowwoc.anchor4j.digitalocean.compute.internal.resource to
		io.github.cowwoc.anchor4j.digitalocean.database, io.github.cowwoc.anchor4j.digitalocean.kubernetes;
	exports io.github.cowwoc.anchor4j.digitalocean.compute.internal.util to
		io.github.cowwoc.anchor4j.digitalocean.kubernetes, io.github.cowwoc.anchor4j.digitalocean.database;
}