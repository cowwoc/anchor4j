package test.io.github.cowwoc.anchor4j.digitalocean.compute;

import io.github.cowwoc.anchor4j.core.exception.AccessDeniedException;
import io.github.cowwoc.anchor4j.core.migration.DriftDetection;
import io.github.cowwoc.anchor4j.core.migration.Migration;
import io.github.cowwoc.anchor4j.digitalocean.compute.client.ComputeClient;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletImage;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;

import java.io.IOException;
import java.nio.file.Path;

public final class V1__Create_Droplet implements Migration
{
	@Override
	public void migrate(DriftDetection driftDetection)
		throws IOException, InterruptedException, AccessDeniedException
	{
		try (ComputeClient client = ComputeClient.build(driftDetection))
		{
			Configuration configuration = Configuration.fromPath(Path.of("test.properties"));
			String accessToken = configuration.getString("ACCESS_TOKEN");
			client.login(accessToken);

			DropletType.Id type = DropletType.id("s-1vcpu-512mb-10gb");
			DropletImage image = client.getDropletImage(candidate ->
				candidate.getSlug().equals("ubuntu-24-10-x64"));
			client.createDroplet("server", type, image.getId()).apply();
		}
	}
}