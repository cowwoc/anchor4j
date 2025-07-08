package io.github.cowwoc.anchor4j.digitalocean.compute.internal.resource;

import io.github.cowwoc.anchor4j.digitalocean.compute.resource.ComputeRegion;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;

import java.util.Set;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultComputeRegion implements ComputeRegion
{
	private final Id id;
	private final String name;
	private final Set<ComputeRegion.Feature> features;
	private final boolean canCreateDroplets;
	private final Set<DropletType.Id> dropletTypes;

	/**
	 * Creates a Region.
	 *
	 * @param id                the region's ID
	 * @param name              the name of this region
	 * @param features          features that are available in this region
	 * @param canCreateDroplets {@code true} if new Droplets can be created in this region
	 * @param dropletTypes      the types of Droplets that can be created in this region
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code name} contains leading or trailing whitespace or is empty
	 */
	public DefaultComputeRegion(Id id, String name, Set<ComputeRegion.Feature> features,
		boolean canCreateDroplets, Set<DropletType.Id> dropletTypes)
	{
		requireThat(id, "id").isNotNull();
		requireThat(name, "name").isStripped().isNotEmpty();
		requireThat(features, "features").isNotNull();
		requireThat(dropletTypes, "dropletTypes").isNotNull();

		this.id = id;
		this.name = name;
		this.features = Set.copyOf(features);
		this.canCreateDroplets = canCreateDroplets;
		this.dropletTypes = Set.copyOf(dropletTypes);
	}

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public Set<Feature> getFeatures()
	{
		return features;
	}

	/**
	 * Determines if new Droplets can be created in this region.
	 *
	 * @return {@code true} if new Droplets can be created
	 */
	public boolean canCreateDroplets()
	{
		return canCreateDroplets;
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Region other && other.getId().equals(id);
	}

	@Override
	public String toString()
	{
		return super.toString();
	}
}