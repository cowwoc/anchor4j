package io.github.cowwoc.anchor4j.digitalocean.network.internal.resource;

import io.github.cowwoc.anchor4j.core.internal.util.ToStringBuilder;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;

import java.util.Objects;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class DefaultVpc implements Vpc
{
	private final Id id;
	private final Region.Id region;

	/**
	 * Creates a new VPC.
	 *
	 * @param id     the ID of the VPC
	 * @param region the region that the VPC is in
	 * @throws NullPointerException if any of the arguments are null
	 */
	public DefaultVpc(Id id, Region.Id region)
	{
		requireThat(id, "id").isNotNull();
		requireThat(region, "region").isNotNull();
		this.id = id;
		this.region = region;
	}

	@Override
	public Id getId()
	{
		return id;
	}

	@Override
	public Region.Id getRegion()
	{
		return region;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, region);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Vpc other && other.getId().equals(id) && other.getRegion().equals(region);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(DefaultVpc.class).
			add("id", id).
			add("region", region).
			toString();
	}
}