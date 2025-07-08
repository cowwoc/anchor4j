package io.github.cowwoc.anchor4j.digitalocean.database.resource;

import io.github.cowwoc.anchor4j.core.id.StringId;
import io.github.cowwoc.anchor4j.core.internal.util.Strings;
import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.core.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A database cluster.
 */
public interface Database
{
	/**
	 * Creates a new ID.
	 *
	 * @param value the server-side identifier
	 * @return the type-safe identifier for the resource
	 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
	 */
	static Id id(String value)
	{
		if (value == null)
			return null;
		return new Id(value);
	}

	/**
	 * Returns the ID of the cluster.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * Returns the name of the cluster.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the database type.
	 *
	 * @return the type
	 */
	DatabaseType.Id getDatabaseTypeId();

	/**
	 * Returns the version number of the database software.
	 *
	 * @return the version number
	 */
	String getVersion();

	/**
	 * Returns the semantic version number of the database software.
	 *
	 * @return the semantic version number
	 */
	String getSemanticVersion();

	/**
	 * Returns the number of standby nodes in the cluster. The cluster includes one primary node, and may
	 * include one or two standby nodes.
	 *
	 * @return the number of standby nodes
	 */
	int getNumberOfStandbyNodes();

	/**
	 * Returns the machine type of the nodes.
	 *
	 * @return the machine type
	 */
	DropletType.Id getDropletType();

	/**
	 * Returns the region that the cluster is deployed in.
	 *
	 * @return the region
	 */
	Region.Id getRegion();

	/**
	 * Returns the status of the cluster.
	 *
	 * @return the status
	 */
	Status getStatus();

	/**
	 * Returns the VPC that the cluster is deployed in.
	 *
	 * @return {@code null} if it is deployed into the default VPC of the region
	 */
	Vpc.Id getVpc();

	/**
	 * Returns the cluster's tags.
	 *
	 * @return the tags
	 */
	Set<String> getTags();

	/**
	 * Returns the names of databases in the cluster.
	 *
	 * @return the names of databases
	 */
	Set<String> getDatabaseNames();

	/**
	 * Returns the connection details for accessing the OpenSearch dashboard, or {@code null} if this is not an
	 * OpenSearch database.
	 *
	 * @return the connection details
	 */
	OpenSearchDashboard getOpenSearchDashboard();

	/**
	 * Returns the database's private connection endpoint.
	 *
	 * @return the private endpoint
	 */
	Connection getPublicConnection();

	/**
	 * Returns the database's public connection endpoint.
	 *
	 * @return the public endpoint
	 */
	Connection getPrivateConnection();

	/**
	 * Returns the standby instance's public connection endpoint.
	 *
	 * @return the standby instance's public endpoint
	 */
	Connection getStandbyPublicConnection();

	/**
	 * Returns the standby instance's private connection endpoint.
	 *
	 * @return the standby instance's private endpoint
	 */
	Connection getStandbyPrivateConnection();

	/**
	 * Returns the database users.
	 *
	 * @return the users
	 */
	Set<User> getUsers();

	/**
	 * Returns the maintenance schedule policy for the cluster.
	 *
	 * @return the maintenance schedule
	 */
	MaintenanceSchedule getMaintenanceSchedule();

	/**
	 * Sets the cluster's maintenance schedule.
	 *
	 * @param maintenanceSchedule the maintenance schedule
	 * @return the updated database
	 * @throws NullPointerException      if {@code maintenanceSchedule} is null
	 * @throws IllegalArgumentException  if the server does not support applying any of the desired changes
	 * @throws IllegalStateException     if the client is closed
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted while waiting for a response. This can
	 *                                   happen due to shutdown signals.
	 * @throws ResourceNotFoundException if the cluster could not be found
	 * @see DatabaseCreator#copyUnchangeablePropertiesFrom(Database)
	 */
	Database setMaintenanceSchedule(DatabaseCreator.MaintenanceSchedule maintenanceSchedule)
		throws IOException, InterruptedException, ResourceNotFoundException;

	/**
	 * Returns the ID of the project that the cluster is assigned to.
	 *
	 * @return the ID of the project
	 */
	String getProjectId();

	/**
	 * Returns the cluster's firewall rules.
	 *
	 * @return the firewall rules
	 */
	Set<FirewallRule> getFirewallRules();

	/**
	 * Returns the time when this version will no longer be supported.
	 *
	 * @return {@code null} if this version does not have an end-of-life timeline
	 */
	Instant getVersionEndOfLife();

	/**
	 * Returns the time when this version will no longer be available for creating new clusters.
	 *
	 * @return {@code null} if this version does not have an end-of-availability timeline
	 */
	Instant getVersionEndOfAvailability();

	/**
	 * Returns the amount of additional storage (in MiB) that is accessible to the cluster.
	 *
	 * @return the amount of additional storage
	 */
	int getAdditionalStorageInMiB();

	/**
	 * Returns the endpoints for accessing the cluster metrics.
	 *
	 * @return the endpoints
	 */
	Set<Endpoint> getMetricsEndpoints();

	/**
	 * Returns the time the cluster was created.
	 *
	 * @return the time the cluster was created
	 */
	Instant getCreatedAt();

	/**
	 * Reloads the cluster's state.
	 *
	 * @return the updated state
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	@CheckReturnValue
	Database reload() throws IOException, InterruptedException;

	/**
	 * Blocks until the cluster reaches the desired {@code status} or a timeout occurs.
	 *
	 * @param status  the desired status
	 * @param timeout the maximum amount of time to wait
	 * @return the updated cluster
	 * @throws NullPointerException      if {@code status} is null
	 * @throws IllegalStateException     if the client is closed
	 * @throws ResourceNotFoundException if the cluster could not be found
	 * @throws IOException               if an I/O error occurs. These errors are typically transient, and
	 *                                   retrying the request may resolve the issue.
	 * @throws InterruptedException      if the thread is interrupted while waiting for a response. This can
	 *                                   happen due to shutdown signals.
	 * @throws TimeoutException          if the operation times out before the cluster reaches the desired
	 *                                   status. This might indicate network latency or server overload.
	 * @see #waitForDestroy(Duration)
	 */
	Database waitForStatus(Status status, Duration timeout)
		throws ResourceNotFoundException, IOException, InterruptedException, TimeoutException;

	/**
	 * Blocks until the cluster deletion completes.
	 *
	 * @param timeout the maximum amount of time to wait
	 * @throws NullPointerException if {@code timeout} is null
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted while waiting for a response. This can happen
	 *                              due to shutdown signals.
	 * @throws TimeoutException     if the operation times out before the cluster reaches the desired status.
	 *                              This might indicate network latency or server overload.
	 */
	void waitForDestroy(Duration timeout) throws IOException, InterruptedException, TimeoutException;

	/**
	 * Destroys the cluster.
	 *
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	void destroy() throws IOException, InterruptedException;

	/**
	 * A type-safe identifier for this type of resource.
	 * <p>
	 * This adds type-safety to API methods by ensuring that IDs specific to one class cannot be used in place
	 * of IDs belonging to another class.
	 */
	final class Id extends StringId
	{
		/**
		 * @param value a server-side identifier
		 * @throws NullPointerException     if {@code value} is null
		 * @throws IllegalArgumentException if {@code value} contains whitespace or is empty
		 */
		private Id(String value)
		{
			super(value);
		}
	}

	/**
	 * The schedule for when maintenance activities may be performed on the cluster.
	 *
	 * @param hour         the start hour when maintenance may take place
	 * @param day          the day of the week when maintenance may take place
	 * @param pending      determines whether any maintenance is scheduled to be performed in the next
	 *                     maintenance window
	 * @param descriptions a list of strings, each containing information about a pending maintenance update
	 */
	record MaintenanceSchedule(OffsetTime hour, DayOfWeek day, boolean pending, List<String> descriptions)
	{
		/**
		 * Creates a new schedule.
		 *
		 * @param hour         the start hour when maintenance may take place
		 * @param day          the day of the week when maintenance may take place
		 * @param pending      determines whether any maintenance is scheduled to be performed in the next
		 *                     maintenance window
		 * @param descriptions a list of strings, each containing information about a pending maintenance update
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if:
		 *                                  <ul>
		 *                                    <li>one of the {@code descriptions} contains leading or trailing
		 *                                    whitespace or is empty.</li>
		 *                                    <li>{@code hour} contains a non-zero minute, second or nano
		 *                                    component.</li>
		 *                                  </ul>
		 */
		public MaintenanceSchedule(OffsetTime hour, DayOfWeek day, boolean pending, List<String> descriptions)
		{
			requireThat(hour, "hour").isNotNull();
			requireThat(hour.getSecond(), "hour.getSecond()").isZero();
			requireThat(hour.getNano(), "hour.getNano()").isZero();
			requireThat(day, "day").isNotNull();

			requireThat(descriptions, "descriptions").isNotNull();
			for (String description : descriptions)
			{
				requireThat(description, "description").withContext(descriptions, "descriptions").
					isStripped().isNotEmpty();
			}

			this.hour = hour;
			this.day = day;
			this.pending = pending;
			this.descriptions = List.copyOf(descriptions);
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(MaintenanceSchedule.class).
				add("hour", hour).
				add("day", day).
				add("pending", pending).
				add("descriptions", descriptions).
				toString();
		}
	}

	/**
	 * The status of the cluster.
	 */
	enum Status
	{
		/**
		 * The cluster is being created.
		 */
		CREATING,
		/**
		 * The cluster is online.
		 */
		ONLINE,
		/**
		 * The cluster is being resized.
		 */
		RESIZING,
		/**
		 * The cluster is being moved to a new instance due to a configuration or version change.
		 */
		MIGRATING,
		/**
		 * The cluster is being copied, usually for backup purposes.
		 */
		FORKING
	}

	/**
	 * Connection details for an OpenSearch dashboard.
	 *
	 * @param uri      the URI of the dashboard
	 * @param hostname the hostname of the dashboard
	 * @param port     the port of the dashboard
	 * @param username the name of the default user
	 * @param password the password of the default user
	 * @param ssl      {@code true} if the connection should be made over SSL
	 */
	record OpenSearchDashboard(URI uri, String hostname, int port, String username, String password,
	                           boolean ssl)
	{
		/**
		 * Creates a new OpenSearchDashboard.
		 *
		 * @param uri      the URI of the dashboard
		 * @param hostname the hostname of the dashboard
		 * @param port     the port of the dashboard
		 * @param username the name of the default user
		 * @param password the password of the default user
		 * @param ssl      {@code true} if the connection should be made over SSL
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if:
		 *                                  <ul>
		 *                                    <li>if any of the arguments contain leading or trailing
		 *                                    whitespace or are empty.</li>
		 *                                    <li>if {@code port} is negative or zero.</li>
		 *                                  </ul>
		 */
		public OpenSearchDashboard
		{
			requireThat(uri, "uri").isNotNull();
			requireThat(username, "userName").isStripped().isNotEmpty();
			requireThat(password, "password").isStripped().isNotEmpty();
		}
	}

	/**
	 * Connection details for a database endpoint.
	 *
	 * @param uri          the URI of the database connection
	 * @param databaseName the name of the database
	 * @param hostname     the hostname of the cluster's primary database server
	 * @param port         the port of the server
	 * @param username     the name of the default user
	 * @param password     the password of the default user
	 * @param ssl          {@code true} if the connection should be made over SSL
	 */
	record Connection(URI uri, String databaseName, String hostname, int port, String username, String password,
	                  boolean ssl)
	{
		/**
		 * Creates a new Connection.
		 *
		 * @param uri          the URI of the database connection
		 * @param databaseName the name of the database
		 * @param hostname     the hostname of the cluster's primary database server
		 * @param port         the port of the server
		 * @param username     the name of the default user
		 * @param password     the password of the default user
		 * @param ssl          {@code true} if the connection should be made over SSL
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if:
		 *                                  <ul>
		 *                                    <li>if any of the arguments contain leading or trailing
		 *                                    whitespace or are empty.</li>
		 *                                    <li>if {@code port} is negative or zero.</li>
		 *                                  </ul>
		 */
		public Connection
		{
			requireThat(uri, "uri").isNotNull();
			requireThat(databaseName, "databaseName").isStripped().isNotEmpty();
			requireThat(username, "username").isStripped().isNotEmpty();
			requireThat(password, "password").isStripped().isNotEmpty();
		}
	}

	/**
	 * A database user.
	 *
	 * @param name               the name of the user
	 * @param role               the role of the user
	 * @param password           the user's password
	 * @param mySqlSettings      (optional) MySQL-specific settings, or null on other databases
	 * @param postgresqlSettings (optional) PostgreSQL-specific settings, or null on other databases
	 * @param openSearchSettings (optional) OpenSearch-specific settings, or null on other databases
	 * @param kafkaSettings      (optional) Kafka-specific settings, or null on other databases
	 */
	record User(String name, UserRole role, String password, MySqlSettings mySqlSettings,
	            PostgresqlSettings postgresqlSettings, OpenSearchSettings openSearchSettings,
	            KafkaSettings kafkaSettings)
	{
		/**
		 * Creates a new User
		 *
		 * @param name               the name of the user
		 * @param role               the role of the user
		 * @param password           the user's password
		 * @param mySqlSettings      (optional) MySQL-specific settings, or null on other databases
		 * @param postgresqlSettings (optional) PostgreSQL-specific settings, or null on other databases
		 * @param openSearchSettings (optional) OpenSearch-specific settings, or null on other databases
		 * @param kafkaSettings      (optional) Kafka-specific settings, or null on other databases
		 * @throws NullPointerException     if any mandatory arguments are null
		 * @throws IllegalArgumentException if any of the arguments contain leading or trailing whitespace or are
		 *                                  empty
		 */
		public User
		{
			requireThat(name, "name").isStripped().isNotEmpty();
			requireThat(role, "role").isNotNull();
			requireThat(password, "password").isStripped().isNotEmpty();
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(User.class).
				add("name", name).
				add("role", role).
				add("password", password).
				add("mySqlSettings", mySqlSettings).
				add("postgresqlSettings", postgresqlSettings).
				add("openSearchSettings", openSearchSettings).
				add("kafkaSettings", kafkaSettings).
				toString();
		}
	}

	/**
	 * Database user roles.
	 */
	enum UserRole
	{
		/**
		 * A system administrator.
		 */
		ADMIN,
		/**
		 * A normal user.
		 */
		NORMAL
	}

	/**
	 * MySQL-specific user settings.
	 *
	 * @param authenticationType the authentication method that must be used to connect to the database
	 */
	record MySqlSettings(MySqlAuthenticationType authenticationType)
	{
		/**
		 * Creates a new MySqlSettings.
		 *
		 * @param authenticationType the authentication method that must be used to connect to the database
		 * @throws NullPointerException if {@code authenticationType} is null
		 */
		public MySqlSettings
		{
			requireThat(authenticationType, "authenticationType").isNotNull();
		}
	}

	/**
	 * Authentication types for the MySQL database.
	 */
	enum MySqlAuthenticationType
	{
		/**
		 * The <a
		 * href="https://dev.mysql.com/doc/refman/8.4/en/native-pluggable-authentication.html">mysql_native_password</a>
		 * authentication method.
		 * <p>
		 * In MySQL 8.0, the default authentication plugin has changed from {@code mysql_native_password} to
		 * {@code caching_sha2_password}.
		 * <p>
		 * The {@code mysql_native_password} plugin is deprecated as of MySQL 8.0.34, disabled by default as of
		 * MySQL 8.4.0, and removed as of MySQL 9.0.0.
		 */
		NATIVE_PASSWORD,
		/**
		 * The <a
		 * href="https://dev.mysql.com/doc/refman/9.1/en/caching-sha2-pluggable-authentication.html">caching_sha2_password</a>
		 * authentication method.
		 */
		CACHING_SHA2_PASSWORD
	}

	/**
	 * PostgreSQL-specific user settings.
	 *
	 * @param mayReplicate {@code true} if the user has replication rights
	 */
	record PostgresqlSettings(boolean mayReplicate)
	{
	}

	/**
	 * OpenSearch-specific user settings.
	 *
	 * @param indexToPermission a mapping from a regular expression to a permission. The key is a regex pattern
	 *                          that matches index names, and the value specifies the permission level for the
	 *                          matching indexes.
	 */
	record OpenSearchSettings(Map<String, OpenSearchPermission> indexToPermission)
	{
		/**
		 * Creates a new OpenSearchSettings.
		 *
		 * @param indexToPermission a mapping from a regular expression to a permission. The key is a regex
		 *                          pattern that matches index names, and the value specifies the permission level
		 *                          for the matching indexes.
		 * @throws NullPointerException if {@code indexToPermission} is null
		 */
		public OpenSearchSettings
		{
			requireThat(indexToPermission, "indexToPermission").isNotNull();
		}
	}

	/**
	 * Permissions that may be granted to a user on OpenSearch databases.
	 */
	enum OpenSearchPermission
	{
		/**
		 * The user cannot access the indexes.
		 */
		DENY,
		/**
		 * The user may read from the indexes.
		 */
		READ,
		/**
		 * The user may write to the indexes.
		 */
		WRITE,
		/**
		 * The user may read and write to the indexes.
		 */
		READ_WRITE,
		/**
		 * The user may read, write and administer the indexes.
		 */
		ADMIN
	}

	/**
	 * Kafka-specific user settings.
	 *
	 * @param certificate access certificate for TLS client authentication
	 * @param key         access key for TLS client authentication
	 * @param permissions maps topics to the permission that is granted to the user over those topics
	 */
	record KafkaSettings(String certificate, String key, Set<KafkaTopicToPermission> permissions)
	{
		/**
		 * Creates a new KafkaSettings.
		 *
		 * @param certificate access certificate for TLS client authentication
		 * @param key         access key for TLS client authentication
		 * @param permissions maps topics to the permission that is granted to the user over those topics
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if any of the arguments contain leading or trailing whitespace or are
		 *                                  empty
		 */
		public KafkaSettings
		{
			requireThat(certificate, "certificate").isStripped().isNotEmpty();
			requireThat(key, "key").isStripped().isNotEmpty();
			requireThat(permissions, "permissions").isNotNull();
		}
	}

	/**
	 * Kafka-specific permissions.
	 *
	 * @param id         the ID of the ACL
	 * @param topics     a regex pattern that matches the topic names
	 * @param permission the permission that is granted to the user over the topics
	 */
	record KafkaTopicToPermission(String id, String topics, KafkaPermission permission)
	{
		/**
		 * Creates a new KafkaTopicToPermission.
		 *
		 * @param id         the ID of the ACL
		 * @param topics     a regex pattern that matches the topic names
		 * @param permission the permission that is granted to the user over the topics
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if any of the arguments contain leading or trailing whitespace or are
		 *                                  empty
		 */
		public KafkaTopicToPermission
		{
			requireThat(id, "id").isStripped().isNotEmpty();
			requireThat(topics, "topics").isStripped().isNotEmpty();
			requireThat(permission, "permission").isNotNull();
		}
	}

	/**
	 * Permissions that may be granted to a user on Kafka databases.
	 */
	enum KafkaPermission
	{
		/**
		 * The user may consume messages from the topics.
		 */
		CONSUME,
		/**
		 * The user may publish messages to the topics.
		 */
		PRODUCE,
		/**
		 * The user may consume or publish messages to the topics.
		 */
		PRODUCE_CONSUME,
		/**
		 * The user may consume, publish and administer the topics.
		 */
		ADMIN
	}

	/**
	 * A firewall rule.
	 */
	record FirewallRule(String id, String clusterId, ResourceType resourceType, String resourceId,
	                    Instant createdAt)
	{
		/**
		 * Creates a new FirewallRule.
		 *
		 * @param id           the ID of the rule
		 * @param clusterId    the ID of the cluster
		 * @param resourceType the type of resource that is allowed to access the database cluster
		 * @param resourceId   the ID of the specific resource, the name of a tag applied to a group of resources,
		 *                     or the IP address
		 * @param createdAt    the time the firewall rule was created
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if:
		 *                                  <ul>
		 *                                    <li>{@code id} is not a valid UUID per RFC 9562.</li>
		 *                                    <li>any of the arguments contains leading or trailing whitespace
		 *                                    or are empty.</li>
		 *                                  </ul>
		 */
		public FirewallRule
		{
			// Regex taken from
			// https://docs.digitalocean.com/reference/api/digitalocean/#tag/Databases/operation/databases_create_cluster
			requireThat(id, "id").matches(Strings.UUID);
			requireThat(clusterId, "clusterId").matches(Strings.UUID);
			requireThat(resourceType, "resourceType").isNotNull();
			requireThat(resourceId, "resourceId").isStripped().isNotEmpty();
			requireThat(createdAt, "createdAt").isNotNull();
		}
	}
}