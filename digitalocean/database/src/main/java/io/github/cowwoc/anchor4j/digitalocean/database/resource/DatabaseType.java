package io.github.cowwoc.anchor4j.digitalocean.database.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Database <a href="https://slugs.do-api.dev/">types</a>.
 *
 * @param id                         this type's ID
 * @param regions                    the regions that are available for this database type
 * @param versions                   the versions that are available for this database type
 * @param nodeCountToDropletTypes    the number and types of droplets supported by this database type. For
 *                                   each number of nodes (key), it shows the types of droplets that can be
 *                                   used.
 * @param versionToEndOfLife         a map from each version to the day that the version will no longer be
 *                                   supported
 * @param versionToEndOfAvailability a map from each version to the day that the version will no longer be
 *                                   available for creating new clusters.
 */
public record DatabaseType(Id id, Set<Region.Id> regions, Set<String> versions,
                           Map<Integer, Set<DropletType.Id>> nodeCountToDropletTypes,
                           Map<String, Instant> versionToEndOfLife,
                           Map<String, Instant> versionToEndOfAvailability)
{
	public DatabaseType(Id id, Set<Region.Id> regions, Set<String> versions,
		Map<Integer, Set<DropletType.Id>> nodeCountToDropletTypes, Map<String, Instant> versionToEndOfLife,
		Map<String, Instant> versionToEndOfAvailability)
	{
		requireThat(id, "id").isNotNull();
		requireThat(regions, "regions").isNotNull();
		requireThat(versions, "versions").isNotNull();
		requireThat(nodeCountToDropletTypes, "nodeCountToDropletTypes").isNotNull();
		requireThat(versionToEndOfLife, "versionToEndOfLife").isNotNull();
		requireThat(versionToEndOfAvailability, "versionToEndOfAvailability").isNotNull();

		this.id = id;
		this.regions = Set.copyOf(regions);
		this.versions = Set.copyOf(versions);
		this.nodeCountToDropletTypes = Map.copyOf(nodeCountToDropletTypes);
		this.versionToEndOfLife = Map.copyOf(versionToEndOfLife);
		this.versionToEndOfAvailability = Map.copyOf(versionToEndOfAvailability);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DatabaseType.class).
			add("id", id).
			add("regions", regions).
			add("versions", versions).
			add("nodeCountToDropletTypes", nodeCountToDropletTypes).
			add("versionToEndOfLife", versionToEndOfLife).
			add("versionToEndOfAvailability", versionToEndOfAvailability).
			toString();
	}

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	public enum Id
	{
		/**
		 * <a href="https://www.postgresql.org/">PostgreSQL</a>.
		 */
		POSTGRESQL,
		/**
		 * <a href="https://www.mysql.com/">MySQL</a>.
		 */
		MYSQL,
		/**
		 * <a href="https://redis.io/">Redis</a>.
		 */
		REDIS,
		/**
		 * <a href="https://www.mongodb.com/">MongoDB</a>.
		 */
		MONGODB,
		/**
		 * <a href="https://kafka.apache.org/">Kafka</a>.
		 */
		KAFKA,
		/**
		 * <a href="https://opensearch.org/">OpenSearch</a>.
		 */
		OPENSEARCH
	}
}