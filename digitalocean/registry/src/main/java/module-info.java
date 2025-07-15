module io.github.cowwoc.anchor4j.digitalocean.registry
{
	requires transitive io.github.cowwoc.anchor4j.digitalocean.core;
	requires io.github.cowwoc.anchor4j.container.core;
	requires io.github.cowwoc.pouch.core;
	requires io.github.cowwoc.requirements12.java;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires org.eclipse.jetty.client;

	exports io.github.cowwoc.anchor4j.digitalocean.registry.client;
	exports io.github.cowwoc.anchor4j.digitalocean.registry.resource;
}