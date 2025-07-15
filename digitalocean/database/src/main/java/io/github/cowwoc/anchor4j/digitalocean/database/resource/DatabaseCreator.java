package io.github.cowwoc.anchor4j.digitalocean.database.resource;

import io.github.cowwoc.anchor4j.core.internal.util.Strings;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.client.ComputeClient;
import io.github.cowwoc.anchor4j.core.exception.AccessDeniedException;
import io.github.cowwoc.anchor4j.digitalocean.core.util.CreateResult;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region.Id;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.OffsetTime;
import java.util.Collection;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Creates a new database cluster.
 */
public interface DatabaseCreator
{
	/**
	 * Sets the major version number of the database software.
	 *
	 * @param version an empty string if the unspecified default value should be used
	 * @return this
	 * @throws IllegalArgumentException if {@code version} contains leading or trailing whitespace
	 */
	DatabaseCreator version(String version);

	/**
	 * Sets the VPC to deploy the cluster in.
	 *
	 * @param vpc {@code null} to deploy the cluster into the region's
	 *            {@link ComputeClient#getDefaultVpc(Id) default} VPC
	 * @return this
	 */
	DatabaseCreator vpc(Vpc.Id vpc);

	/**
	 * Adds a tag to the cluster.
	 *
	 * @param tag the tag to add
	 * @return this
	 * @throws NullPointerException     if {@code tag} is null
	 * @throws IllegalArgumentException if the tag:
	 *                                  <ul>
	 *                                    <li>contains any characters other than letters, numbers, colons,
	 *                                    dashes and underscores.</li>
	 *                                    <li>is longer than 255 characters.</li>
	 *                                  </ul>
	 */
	DatabaseCreator tag(String tag);

	/**
	 * Sets the tags of the cluster.
	 *
	 * @param tags the tags
	 * @return this
	 * @throws NullPointerException     if {@code tags} is null
	 * @throws IllegalArgumentException if any of the tags:
	 *                                  <ul>
	 *                                    <li>are null.</li>
	 *                                    <li>contain any characters other than letters, numbers, colons,
	 *                                    dashes and underscores.</li>
	 *                                    <li>is longer than 255 characters.</li>
	 *                                  </ul>
	 */
	DatabaseCreator tags(Collection<String> tags);

	/**
	 * Sets the ID of the project that the cluster is assigned to.
	 *
	 * @param projectId an empty string to assign the cluster to the default project
	 * @return this
	 * @throws NullPointerException     if {@code projectId} is null
	 * @throws IllegalArgumentException if {@code projectId} contains leading or trailing whitespace
	 */
	DatabaseCreator projectId(String projectId);

	/**
	 * Adds a firewall rule to the cluster.
	 *
	 * @param firewallRule the rule
	 * @return this
	 * @throws NullPointerException if {@code rule} is null
	 */
	DatabaseCreator firewallRule(FirewallRuleBuilder firewallRule);

	/**
	 * Sets the firewall rules of the cluster.
	 *
	 * @param firewallRules the firewall rules
	 * @return this
	 * @throws NullPointerException     if {@code firewallRules} is null
	 * @throws IllegalArgumentException if any of the rules are null
	 */
	DatabaseCreator firewallRules(Collection<FirewallRuleBuilder> firewallRules);

	/**
	 * Sets the amount of additional storage that is accessible to the cluster.
	 *
	 * @param additionalStorageInMiB the amount in MiB
	 * @throws IllegalArgumentException if {@code additionalStorageInMiB} is negative
	 */
	void additionalStorageInMiB(int additionalStorageInMiB);

	/**
	 * Sets the backup to restore data from.
	 *
	 * @param restoreFrom {@code null} to create an empty cluster
	 * @return this
	 * @throws NullPointerException if {@code restoreFrom} is null
	 */
	DatabaseCreator restoreFrom(RestoreFrom restoreFrom);

	/**
	 * Creates a new database cluster.
	 *
	 * @return the new or conflicting cluster
	 * @throws IllegalArgumentException if the {@code dropletType} is not supported by managed databases
	 * @throws IllegalStateException    if the client is closed
	 * @throws AccessDeniedException    if the client does not have sufficient privileges to execute this
	 *                                  request
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	CreateResult<Database> apply() throws AccessDeniedException, IOException, InterruptedException;

	/**
	 * Builds a firewall rule.
	 *
	 * @param id           (optional) the ID of the rule, or an empty string to omit
	 * @param resourceType the type of resource that is allowed to access the database cluster
	 * @param resourceId   the ID of the specific resource, the name of a tag applied to a group of resources,
	 *                     or the IP address
	 */
	record FirewallRuleBuilder(String id, ResourceType resourceType, String resourceId)
	{
		/**
		 * Creates a FirewallRule.
		 *
		 * @param id           (optional) the ID of the rule, or an empty string to omit
		 * @param resourceType the type of resource that is allowed to access the database cluster
		 * @param resourceId   the ID of the specific resource, the name of a tag applied to a group of resources,
		 *                     or the IP address
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if:
		 *                                  <ul>
		 *                                    <li>{@code id} is not a valid UUID per RFC 9562.</li>
		 *                                    <li>any of the arguments contains leading or trailing whitespace
		 *                                    or are empty.</li>
		 *                                  </ul>
		 */
		public FirewallRuleBuilder
		{
			requireThat(id, "id").isStripped();
			if (!id.isEmpty())
				requireThat(id, "id").matches(Strings.UUID);
			requireThat(resourceType, "resourceType").isNotNull();
			requireThat(resourceId, "resourceId").isStripped().isNotEmpty();
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(FirewallRuleBuilder.class).
				add("id", id).
				add("resourceType", resourceType).
				add("resourceId", resourceId).
				toString();
		}
	}

	/**
	 * Identifies a backup to restore from.
	 */
	record RestoreFrom(String databaseName, Instant createdAt)
	{
		/**
		 * Creates a RestoreFrom.
		 *
		 * @param databaseName the name of an existing database cluster from which the backup will be restored
		 * @param createdAt    the time that the backup was created
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code databaseName} contains leading or trailing whitespace or is
		 *                                  empty
		 */
		public RestoreFrom
		{
			requireThat(databaseName, "databaseName").isStripped().isNotEmpty();
			requireThat(createdAt, "createdAt").isNotNull();
		}

		/**
		 * Returns the name of an existing database cluster from which the backup will be restored.
		 *
		 * @return the name
		 */
		public String getDatabaseName()
		{
			return databaseName;
		}

		/**
		 * Returns the time that the backup was created.
		 *
		 * @return the time
		 */
		public Instant getCreatedAt()
		{
			return createdAt;
		}
	}

	/**
	 * The schedule for when maintenance activities may be performed on the cluster.
	 */
	record MaintenanceSchedule(OffsetTime hour, DayOfWeek day)
	{
		/**
		 * Creates a new schedule.
		 *
		 * @param hour the start hour when maintenance may take place
		 * @param day  the day of the week when maintenance may take place
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code hour} contains a non-zero minute, second or nano component
		 */
		public MaintenanceSchedule
		{
			requireThat(hour, "hour").isNotNull();
			requireThat(hour.getMinute(), "hour.getMinute()").isZero();
			requireThat(hour.getSecond(), "hour.getSecond()").isZero();
			requireThat(hour.getNano(), "hour.getNano()").isZero();
			requireThat(day, "day").isNotNull();
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(MaintenanceSchedule.class).
				add("hour", hour).
				add("day", day).
				toString();
		}
	}
}