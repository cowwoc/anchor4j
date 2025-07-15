module io.github.cowwoc.anchor4j.digitalocean.kubernetes
{
	requires transitive io.github.cowwoc.anchor4j.digitalocean.compute;
	requires io.github.cowwoc.anchor4j.digitalocean.network;
	requires io.github.cowwoc.requirements12.java;
	requires com.fasterxml.jackson.databind;
	requires org.slf4j;
	requires org.eclipse.jetty.client;

	exports io.github.cowwoc.anchor4j.digitalocean.kubernetes.client;
	exports io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource;
}