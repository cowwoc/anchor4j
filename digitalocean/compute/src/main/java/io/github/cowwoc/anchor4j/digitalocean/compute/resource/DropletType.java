package io.github.cowwoc.anchor4j.digitalocean.compute.resource;

import io.github.cowwoc.anchor4j.core.id.StringId;

import java.math.BigDecimal;
import java.util.Set;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Droplet types.
 *
 * @see <a href="https://www.digitalocean.com/pricing/droplets#basic-droplets">pricing</a>
 */
public interface DropletType
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
	 * Returns the type's ID.
	 *
	 * @return the ID
	 */
	Id getId();

	/**
	 * Returns the amount of RAM allocated to this type, in MiB.
	 *
	 * @return the amount of RAM
	 */
	int getRamInMiB();

	/**
	 * Returns the number of virtual CPUs allocated to this type. Note that vCPUs are relative units specific to
	 * each vendor's infrastructure and hardware. A number of vCPUs is relative to other vCPUs by the same
	 * vendor, meaning that the allocation and performance are comparable within the same vendor's environment.
	 *
	 * @return the number of virtual CPUs
	 */
	int getCpus();

	/**
	 * Returns the amount of disk space allocated to this type, in GiB.
	 *
	 * @return the amount of disk space
	 */
	int getDiskInMiB();

	/**
	 * Returns the amount of outgoing network traffic allocated to this type, in GiB.
	 *
	 * @return the amount of network traffic
	 */
	BigDecimal getTransferInGiB();

	/**
	 * Returns the hourly cost of this Droplet type in US dollars. This cost is incurred as long as the Droplet
	 * is active and has not been destroyed.
	 *
	 * @return the cost per hour
	 */
	BigDecimal getCostPerHour();

	/**
	 * Returns the monthly cost of this Droplet type in US dollars. This cost is incurred as long as the Droplet
	 * is active and has not been destroyed.
	 *
	 * @return the cost per month
	 */
	BigDecimal getCostPerMonth();

	/**
	 * Returns the regions where this type of Droplet may be created.
	 *
	 * @return the regions
	 */
	Set<ComputeRegion.Id> getRegions();

	/**
	 * Determines if Droplets may be created with this type, regardless of the region.
	 *
	 * @return {@code true} if Droplets may be created
	 */
	boolean isAvailable();

	/**
	 * Returns a description of this type. For example, Basic, General Purpose, CPU-Optimized, Memory-Optimized,
	 * or Storage-Optimized.
	 *
	 * @return the description
	 */
	String getDescription();

	/**
	 * Returns the disks available to this type.
	 *
	 * @return the details
	 */
	Set<DiskConfiguration> getDiskConfiguration();

	/**
	 * Returns the GPU available to this type.
	 *
	 * @return {@code null} if absent
	 */
	GpuConfiguration getGpuConfiguration();

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
	 * Describes the disk that is allocated to a droplet.
	 *
	 * @param persistent {@code true} if the disk is persistent
	 * @param sizeInMiB  the amount of space allocated to this disk, in MiB
	 */
	record DiskConfiguration(boolean persistent, int sizeInMiB)
	{
		/**
		 * Creates a new DiskConfiguration.
		 *
		 * @param persistent {@code true} if the disk is persistent
		 * @param sizeInMiB  the amount of space allocated to this disk, in MiB
		 * @throws IllegalArgumentException if {@code sizeInMiB} is negative or zero
		 */
		public DiskConfiguration
		{
			requireThat(sizeInMiB, "sizeInMiB").isPositive();
		}
	}

	/**
	 * Describes the GPU that is allocated to a droplet type.
	 *
	 * @param count    the number of GPUs allocated to this type of droplet
	 * @param model    the model of the GPU
	 * @param ramInMiB the size of the GPU's RAM, in MiB
	 */
	record GpuConfiguration(int count, String model, int ramInMiB)
	{
		/**
		 * Creates a new instance.
		 *
		 * @param count    the number of GPUs allocated to this type of droplet
		 * @param model    the model of the GPU
		 * @param ramInMiB the size of the GPU's RAM, in MiB
		 * @throws NullPointerException     if {@code model} is null
		 * @throws IllegalArgumentException if:
		 *                                  <ul>
		 *                                  <li>{@code count} is negative or zero.</li>
		 *                                  <li>{@code model} contains leading or trailing whitespace or is empty.</li>
		 *                                  <li>{@code ramInMb} is negative or zero.</li>
		 *                                  </ul>
		 */
		public GpuConfiguration
		{
			requireThat(count, "count").isPositive();
			requireThat(model, "model").isStripped().isNotEmpty();
			requireThat(ramInMiB, "ramInMb").isPositive();
		}
	}
}