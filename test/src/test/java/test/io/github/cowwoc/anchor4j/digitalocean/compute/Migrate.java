package test.io.github.cowwoc.anchor4j.digitalocean.compute;

import io.github.cowwoc.anchor4j.core.exception.AccessDeniedException;
import io.github.cowwoc.anchor4j.core.migration.DriftDetection;

import java.io.IOException;

/**
 * Runs cloud migrations.
 */
public final class Migrate
{
	public static void main(String[] args) throws IOException, InterruptedException, AccessDeniedException
	{
		try (DriftDetection client = DriftDetection.build())
		{
			V1__Create_Droplet v1 = new V1__Create_Droplet();
			v1.migrate(client);
		}
	}
}