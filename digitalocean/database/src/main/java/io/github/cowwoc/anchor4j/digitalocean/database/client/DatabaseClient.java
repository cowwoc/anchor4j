package io.github.cowwoc.anchor4j.digitalocean.database.client;

import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.core.client.DigitalOceanClient;
import io.github.cowwoc.anchor4j.digitalocean.database.internal.client.DefaultDatabaseClient;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.Database;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.Database.Id;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.DatabaseCreator;
import io.github.cowwoc.anchor4j.digitalocean.database.resource.DatabaseType;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/**
 * A DigitalOcean database client.
 */
public interface DatabaseClient extends DigitalOceanClient
{
	/**
	 * Returns a client.
	 *
	 * @return the client
	 * @throws IOException if an I/O error occurs while building the client
	 */
	static DatabaseClient build() throws IOException
	{
		return new DefaultDatabaseClient();
	}

	/**
	 * Looks up a database type.
	 *
	 * @param id the ID of the type
	 * @return the matching value
	 * @throws IllegalArgumentException if no match is found
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	DatabaseType getDatabaseType(DatabaseType.Id id) throws IOException, InterruptedException;

	/**
	 * Returns the database clusters.
	 *
	 * @return an empty list if no match is found
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<Database> getDatabaseClusters() throws IOException, InterruptedException;

	/**
	 * Returns the databases that matches a predicate.
	 *
	 * @param predicate the predicate
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code predicate} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<Database> getDatabaseClusters(Predicate<Database> predicate) throws IOException, InterruptedException;

	/**
	 * Looks up a database cluster by its ID.
	 *
	 * @param id the ID
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code id} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Database getDatabaseCluster(Id id) throws IOException, InterruptedException;

	/**
	 * Returns the first database that matches a predicate.
	 *
	 * @param predicate the predicate
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code predicate} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Database getDatabaseCluster(Predicate<Database> predicate) throws IOException, InterruptedException;

	/**
	 * Creates a database cluster.
	 *
	 * @param name                 the name of the cluster
	 * @param databaseType         the type of the database
	 * @param numberOfStandbyNodes the number of standby nodes in the cluster. The cluster includes one primary
	 *                             node, and may include one or two standby nodes.
	 * @param dropletType          the machine type of the droplet
	 * @param region               the region to create the cluster in. To create a cluster that spans multiple
	 *                             regions, add DatabaseReplica to the cluster.
	 * @return a new database creator
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code name} contains leading or trailing whitespace or is
	 *                                    empty.</li>
	 *                                    <li>{@code numberOfStandbyNodes} is negative, or greater than 2.</li>
	 *                                  </ul>
	 * @throws IllegalStateException    if the client is closed
	 */
	@CheckReturnValue
	DatabaseCreator createDatabaseCluster(DatabaseClient client, String name, DatabaseType databaseType,
		int numberOfStandbyNodes, DropletType.Id dropletType, Region.Id region);
}