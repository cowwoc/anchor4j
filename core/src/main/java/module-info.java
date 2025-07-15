/**
 * Code that is common to all modules.
 */
module io.github.cowwoc.anchor4j.core
{
	requires io.github.cowwoc.pouch.core;
	requires io.github.cowwoc.requirements12.java;
	requires io.github.cowwoc.requirements12.jackson;
	requires org.slf4j;
	requires com.fasterxml.jackson.databind;
	requires org.threeten.extra;

	exports io.github.cowwoc.anchor4j.core.client;
	exports io.github.cowwoc.anchor4j.core.exception;
	exports io.github.cowwoc.anchor4j.core.id;
	exports io.github.cowwoc.anchor4j.core.migration;
	exports io.github.cowwoc.anchor4j.core.resource;

	exports io.github.cowwoc.anchor4j.core.internal.client to
		io.github.cowwoc.anchor4j.container.core, io.github.cowwoc.anchor4j.container.core.test,
		io.github.cowwoc.anchor4j.container.buildx, io.github.cowwoc.anchor4j.container.buildx.test,
		io.github.cowwoc.anchor4j.container.docker, io.github.cowwoc.anchor4j.container.docker.test,
		io.github.cowwoc.anchor4j.digitalocean.core, io.github.cowwoc.anchor4j.digitalocean.registry,
		io.github.cowwoc.anchor4j.digitalocean.compute;

	exports io.github.cowwoc.anchor4j.core.internal.resource to
		io.github.cowwoc.anchor4j.container.core, io.github.cowwoc.anchor4j.container.core.test,
		io.github.cowwoc.anchor4j.container.buildx, io.github.cowwoc.anchor4j.container.buildx.test,
		io.github.cowwoc.anchor4j.container.docker, io.github.cowwoc.anchor4j.container.docker.test,
		io.github.cowwoc.anchor4j.digitalocean.core, io.github.cowwoc.anchor4j.digitalocean.compute,
		io.github.cowwoc.anchor4j.digitalocean.database, io.github.cowwoc.anchor4j.digitalocean.registry,
		io.github.cowwoc.anchor4j.digitalocean.network, io.github.cowwoc.anchor4j.digitalocean.project,
		io.github.cowwoc.anchor4j.digitalocean.kubernetes;

	exports io.github.cowwoc.anchor4j.core.internal.util to
		io.github.cowwoc.anchor4j.container.core, io.github.cowwoc.anchor4j.container.core.test,
		io.github.cowwoc.anchor4j.container.buildx, io.github.cowwoc.anchor4j.container.buildx.test,
		io.github.cowwoc.anchor4j.container.docker, io.github.cowwoc.anchor4j.container.docker.test,
		io.github.cowwoc.anchor4j.digitalocean.core, io.github.cowwoc.anchor4j.digitalocean.compute,
		io.github.cowwoc.anchor4j.digitalocean.database, io.github.cowwoc.anchor4j.digitalocean.registry,
		io.github.cowwoc.anchor4j.digitalocean.network, io.github.cowwoc.anchor4j.digitalocean.project,
		io.github.cowwoc.anchor4j.digitalocean.kubernetes, io.github.cowwoc.anchor4j.digitalocean.driftdetection;
}