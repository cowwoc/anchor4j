module io.github.cowwoc.anchor4j.digitalocean.database
{
	requires transitive io.github.cowwoc.anchor4j.core;
	requires io.github.cowwoc.anchor4j.digitalocean.core;
	requires io.github.cowwoc.anchor4j.digitalocean.network;
	requires transitive io.github.cowwoc.anchor4j.digitalocean.compute;
	requires io.github.cowwoc.requirements12.java;
	requires org.eclipse.jetty.client;
	requires com.fasterxml.jackson.databind;

	exports io.github.cowwoc.anchor4j.digitalocean.database.client;
	exports io.github.cowwoc.anchor4j.digitalocean.database.resource;
}