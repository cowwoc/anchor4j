module io.github.cowwoc.anchor4j.digitalocean.project
{
	requires transitive io.github.cowwoc.anchor4j.digitalocean.core;
	requires io.github.cowwoc.anchor4j.digitalocean.compute;
	requires io.github.cowwoc.anchor4j.digitalocean.network;
	requires io.github.cowwoc.pouch.core;
	requires io.github.cowwoc.requirements12.java;
	requires com.fasterxml.jackson.databind;

	exports io.github.cowwoc.anchor4j.digitalocean.project.client;
	exports io.github.cowwoc.anchor4j.digitalocean.project.resource;
}