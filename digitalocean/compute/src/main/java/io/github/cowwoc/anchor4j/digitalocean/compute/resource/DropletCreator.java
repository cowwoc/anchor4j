package io.github.cowwoc.anchor4j.digitalocean.compute.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.client.ComputeClient;
import io.github.cowwoc.anchor4j.core.exception.AccessDeniedException;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region.Id;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.OffsetTime;
import java.util.Collection;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Creates a new droplet.
 */
public interface DropletCreator
{
	/***
	 * Sets the VPC to deploy the droplet in.
	 *
	 * @param vpc    {@code null} to deploy the droplet into the region's
	 * {@link ComputeClient#getDefaultVpc(Id) default} VPC
	 * @return this
	 */
	DropletCreator vpc(Vpc.Id vpc);

	/**
	 * Sets the region to create the droplet in.
	 *
	 * @param region the region to create the droplet in
	 * @return this
	 * @throws NullPointerException if {@code region} is null
	 */
	DropletCreator region(Region.Id region);

	/**
	 * Adds an SSH key that may be used to connect to the droplet.
	 *
	 * @param key a public key
	 * @return this
	 */
	DropletCreator sshKey(SshPublicKey key);

	/**
	 * Sets the Droplet's backup schedule.
	 *
	 * @param backupSchedule {@code null} to disable backups
	 * @return this
	 */
	DropletCreator backupSchedule(BackupSchedule backupSchedule);

	/**
	 * Enables a feature on the droplet.
	 *
	 * @param feature the feature to enable
	 * @return this
	 * @throws NullPointerException if {@code feature} is null
	 */
	DropletCreator feature(DropletFeature feature);

	/**
	 * Sets the features that should be enabled on the droplet.
	 *
	 * @param features the features that should be enabled
	 * @return this
	 * @throws NullPointerException     if {@code features} is null
	 * @throws IllegalArgumentException if any of the features are null
	 */
	DropletCreator features(Collection<DropletFeature> features);

	/**
	 * Adds a tag to the droplet.
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
	DropletCreator tag(String tag);

	/**
	 * Sets the tags of the droplet.
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
	DropletCreator tags(Collection<String> tags);

	/**
	 * Sets the droplet's "user data" which may be used to configure the Droplet on the first boot, often a
	 * "cloud-config" file or Bash script. It must be plain text and may not exceed 64 KiB in size.
	 *
	 * @param userData the user data
	 * @return this
	 * @throws NullPointerException     if {@code userData} is null
	 * @throws IllegalArgumentException if {@code userData}:
	 *                                  <ul>
	 *                                    <li>contains leading or trailing whitespace.</li>
	 *                                    <li>is empty.</li>
	 *                                    <li>is longer than 64 KiB characters.</li>
	 *                                  </ul>
	 */
	DropletCreator userData(String userData);

	/**
	 * Determines if the deployment should fail if the web console does not support the Droplet operating
	 * system. The default value is {@code false}.
	 *
	 * @param failOnUnsupportedOperatingSystem {@code true} if the deployment should fail on unsupported
	 *                                         operating systems
	 * @return this
	 */
	DropletCreator failOnUnsupportedOperatingSystem(boolean failOnUnsupportedOperatingSystem);

	/**
	 * Creates a new droplet.
	 *
	 * @return the new droplet
	 * @throws IllegalStateException if the client is closed
	 * @throws AccessDeniedException if the client does not have sufficient privileges to execute this request
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Droplet apply() throws AccessDeniedException, IOException, InterruptedException;

	/**
	 * The schedule for when backup activities may be performed on the droplet.
	 */
	record BackupSchedule(OffsetTime hour, DayOfWeek day, BackupFrequency frequency)
	{
		/**
		 * Creates a new schedule.
		 *
		 * @param hour      the start hour when maintenance may take place
		 * @param day       (optional) the day of the week when maintenance may take place (ignored if
		 *                  {@code frequency} is daily})
		 * @param frequency determines how often backups take place
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code hour} contains a non-zero minute, second or nano component
		 */
		public BackupSchedule(OffsetTime hour, DayOfWeek day, BackupFrequency frequency)
		{
			requireThat(hour, "hour").isNotNull();
			requireThat(hour.getMinute(), "hour.getMinute()").isZero();
			requireThat(hour.getSecond(), "hour.getSecond()").isZero();
			requireThat(hour.getNano(), "hour.getNano()").isZero();

			this.hour = hour;
			this.day = day;
			this.frequency = frequency;
		}

		@Override
		public String toString()
		{
			return new ToStringBuilder(BackupSchedule.class).
				add("hour", hour).
				add("day", day).
				add("frequency", frequency).
				toString();
		}
	}

	/**
	 * Determines how often a backup is created.
	 */
	enum BackupFrequency
	{
		/**
		 * Every day.
		 */
		DAILY,
		/**
		 * Every week.
		 */
		WEEKLY
	}
}