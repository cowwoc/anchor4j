package io.github.cowwoc.anchor4j.digitalocean.compute.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.util.Numbers;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.ComputeRegion;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;

import java.math.BigDecimal;
import java.util.Set;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultDropletType implements DropletType
{
	private final Id id;
	private final int ramInMiB;
	private final int cpus;
	private final int diskInMiB;
	private final BigDecimal transferInGiB;
	private final BigDecimal costPerHour;
	private final BigDecimal costPerMonth;
	private final Set<ComputeRegion.Id> regions;
	private final boolean available;
	private final String description;
	private final Set<DiskConfiguration> diskConfiguration;
	private final GpuConfiguration gpuConfiguration;

	/**
	 * Creates a new instance.
	 *
	 * @param id                the type's ID
	 * @param ramInMiB          the amount of RAM allocated to this type, in MiB
	 * @param cpus              the number of virtual CPUs allocated to this type. Note that vCPUs are relative
	 *                          units specific to each vendor's infrastructure and hardware. A number of vCPUs
	 *                          is relative to other vCPUs by the same vendor, meaning that the allocation and
	 *                          performance are comparable within the same vendor's environment.
	 * @param diskInMiB         the amount of disk space allocated to this type, in GiB
	 * @param transferInGiB     the amount of outgoing network traffic allocated to this type, in GiB
	 * @param costPerHour       the hourly cost of this Droplet type in US dollars. This cost is incurred as
	 *                          long as the Droplet is active and has not been destroyed.
	 * @param costPerMonth      the monthly cost of this Droplet type in US dollars. This cost is incurred as
	 *                          long as the Droplet is active and has not been destroyed.
	 * @param regions           the regions where this type of Droplet may be created
	 * @param available         {@code true} if Droplets may be created with this type, regardless of the
	 *                          region
	 * @param description       a description of this type. For example, Basic, General Purpose, CPU-Optimized,
	 *                          Memory-Optimized, or Storage-Optimized.
	 * @param diskConfiguration describes the disks available to this type
	 * @param gpuConfiguration  (optional) describes the GPU available to this type, or {@code null} if absent
	 * @throws NullPointerException     if any of the mandatory arguments are null
	 * @throws IllegalArgumentException if {@code description} contains leading or trailing whitespace or is
	 *                                  empty
	 */
	public DefaultDropletType(Id id, int ramInMiB, int cpus, int diskInMiB, BigDecimal transferInGiB,
		BigDecimal costPerHour, BigDecimal costPerMonth, Set<ComputeRegion.Id> regions, boolean available,
		String description, Set<DiskConfiguration> diskConfiguration, GpuConfiguration gpuConfiguration)
	{
		requireThat(id, "id").isNotNull();
		requireThat(ramInMiB, "ramInMiB").isPositive();
		requireThat(cpus, "vCpus").isPositive();
		requireThat(diskInMiB, "diskInMiB").isPositive();
		requireThat(transferInGiB, "transferInGiB").isPositive();
		requireThat(costPerHour, "costPerHour").isPositive();
		requireThat(costPerMonth, "costPerMonth").isPositive();
		requireThat(regions, "regions").isNotNull();
		requireThat(description, "description").isStripped().isNotEmpty();
		requireThat(diskConfiguration, "diskConfiguration").isNotNull();
		this.id = id;
		this.ramInMiB = ramInMiB;
		this.cpus = cpus;
		this.diskInMiB = diskInMiB;
		this.transferInGiB = Numbers.copyOf(transferInGiB);
		this.costPerHour = Numbers.copyOf(costPerHour);
		this.costPerMonth = Numbers.copyOf(costPerMonth);
		this.regions = Set.copyOf(regions);
		this.available = available;
		this.description = description;
		this.diskConfiguration = diskConfiguration;
		this.gpuConfiguration = gpuConfiguration;
	}

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public int getRamInMiB()
	{
		return ramInMiB;
	}

	@Override
	public int getCpus()
	{
		return cpus;
	}

	@Override
	public int getDiskInMiB()
	{
		return diskInMiB;
	}

	@Override
	public BigDecimal getTransferInGiB()
	{
		return transferInGiB;
	}

	@Override
	public BigDecimal getCostPerHour()
	{
		return costPerHour;
	}

	@Override
	public BigDecimal getCostPerMonth()
	{
		return costPerMonth;
	}

	@Override
	public Set<ComputeRegion.Id> getRegions()
	{
		return regions;
	}

	@Override
	public boolean isAvailable()
	{
		return available;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public Set<DiskConfiguration> getDiskConfiguration()
	{
		return diskConfiguration;
	}

	@Override
	public GpuConfiguration getGpuConfiguration()
	{
		return gpuConfiguration;
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof DropletType other && other.getId().equals(id);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultDropletType.class).
			add("id", id).
			add("ramInMiB", ramInMiB).
			add("cpus", cpus).
			add("diskInMiB", diskInMiB).
			add("transferInGiB", transferInGiB).
			add("pricePerHour", costPerHour).
			add("pricePerMonth", costPerMonth).
			add("regions", regions).
			add("available", available).
			add("description", description).
			add("diskConfiguration", diskConfiguration).
			add("gpuConfiguration", gpuConfiguration).
			toString();
	}
}