package io.github.cowwoc.anchor4j.container.docker.test.resource;

import io.github.cowwoc.anchor4j.container.core.resource.BuildListener.Output;
import io.github.cowwoc.anchor4j.container.core.resource.Builder;
import io.github.cowwoc.anchor4j.container.core.resource.BuilderCreator.Driver;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImageBuilder.Exporter;
import io.github.cowwoc.anchor4j.container.core.resource.DefaultBuildListener;
import io.github.cowwoc.anchor4j.container.core.resource.WaitFor;
import io.github.cowwoc.anchor4j.container.core.resource.test.TestBuildListener;
import io.github.cowwoc.anchor4j.container.docker.test.IntegrationTestContainer;
import io.github.cowwoc.anchor4j.core.internal.util.Paths;
import io.github.cowwoc.anchor4j.docker.client.DockerClient;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.DockerImageBuilder;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class ImageIT
{
	// Use GitHub Container Registry because Docker Hub's rate-limits are too low
	static final String EXISTING_IMAGE = "ghcr.io/hlesey/busybox";
	static final String MISSING_IMAGE = "ghcr.io/cowwoc/missing";
	static final String FILE_IN_CONTAINER = "logback-test.xml";

	@Test
	public void pull() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		requireThat(image, "image").isNotNull();
		it.onSuccess();
	}

	@Test
	public void alreadyPulled() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image1 = client.pullImage(EXISTING_IMAGE).apply();
		requireThat(image1, "image1").isNotNull();
		DockerImage image2 = client.pullImage(EXISTING_IMAGE).apply();
		requireThat(image1, "image1").isEqualTo(image2, "image2");
		it.onSuccess();
	}

	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void pullMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		try
		{
			client.pullImage(MISSING_IMAGE).apply();
		}
		catch (ResourceNotFoundException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	@Test
	public void listEmpty() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEmpty();
		it.onSuccess();
	}

	@Test
	public void list() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image1 = client.pullImage(EXISTING_IMAGE).apply();
		requireThat(image1, "image1").isNotNull();

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").size().isEqualTo(1);
		DockerImage image2 = images.getFirst();
		requireThat(image1, "image1").isEqualTo(image2, "image2");

		requireThat(image2.referenceToTags().keySet(), "image2.references()").
			isEqualTo(Set.of(EXISTING_IMAGE));
		requireThat(image2.referenceToTags(), "image2.referenceToTags()").
			isEqualTo(Map.of(EXISTING_IMAGE, Set.of("latest")));
		it.onSuccess();
	}

	@Test
	public void tag() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		requireThat(image, "image").isNotNull();
		image = image.addTag("rocket-ship").reload();

		requireThat(image.referenceToTags().keySet(), "image.references()").
			isEqualTo(Set.of(EXISTING_IMAGE, "rocket-ship"));
		it.onSuccess();
	}

	@Test
	public void alreadyTagged() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		requireThat(image, "image").isNotNull();

		image.addTag("rocket-ship");
		image = image.addTag("rocket-ship").reload();

		requireThat(image.referenceToTags().keySet(), "image.references()").
			isEqualTo(Set.of(EXISTING_IMAGE, "rocket-ship"));
		it.onSuccess();
	}

	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void tagMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		requireThat(image, "image").isNotNull();

		image.remove().apply();
		try
		{
			image.addTag("rocket-ship");
		}
		catch (ResourceNotFoundException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	@Test
	public void get() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).apply();
		DockerImage state = client.getImage(ContainerImage.id(EXISTING_IMAGE));
		requireThat(image.getId(), "image.getId()").isEqualTo(state.getId(), "state.getId()");
		it.onSuccess();
	}

	@Test
	public void getMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		DockerImage image = client.getImage(MISSING_IMAGE);
		requireThat(image, "missingImage").isNull();
		it.onSuccess();
	}

	@Test
	public void buildAndExportToDocker() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		DockerImage image = client.buildImage().export(Exporter.dockerImage().build()).apply(buildContext);

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEqualTo(List.of(image));
		it.onSuccess();
	}

	@Test
	public void buildWithCustomDockerfile() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		DockerImage image = client.buildImage().dockerfile(buildContext.resolve("custom/Dockerfile")).
			export(Exporter.dockerImage().build()).
			apply(buildContext);

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEqualTo(List.of(image));
		it.onSuccess();
	}

	@Test(expectedExceptions = FileNotFoundException.class)
	public void buildWithMissingDockerfile() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		try
		{
			client.buildImage().dockerfile(buildContext.resolve("missing/Dockerfile")).apply(buildContext);
		}
		catch (FileNotFoundException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	@Test
	public void buildWithSinglePlatform() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		DockerImage image = client.buildImage().platform("linux/amd64").
			export(Exporter.dockerImage().build()).
			apply(buildContext);

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEqualTo(List.of(image));
		it.onSuccess();
	}

	@Test
	public void buildWithMultiplePlatform() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		DockerImage image = client.buildImage().platform("linux/amd64").platform("linux/arm64").
			export(Exporter.dockerImage().build()).
			apply(buildContext);

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEqualTo(List.of(image));
		it.onSuccess();
	}

	@Test
	public void buildWithTag() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		DockerImage image = client.buildImage().reference("integration-test").
			export(Exporter.dockerImage().build()).
			apply(buildContext);

		requireThat(image.referenceToTags(), "image.referenceToTags()").
			isEqualTo(Map.of("integration-test", Set.of("latest")));

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEqualTo(List.of(image));
		it.onSuccess();
	}

	@Test
	public void buildPassedWithCustomListener() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		TestBuildListener listener = new TestBuildListener();
		client.buildImage().listener(listener).apply(buildContext);
		requireThat(listener.buildStarted.get(), "buildStarted").withContext(listener, "listener").isTrue();
		requireThat(listener.waitUntilBuildCompletes.get(), "waitUntilBuildCompletes").
			withContext(listener, "listener").isTrue();
		requireThat(listener.buildPassed.get(), "buildSucceeded").withContext(listener, "listener").isTrue();
		requireThat(listener.buildFailed.get(), "buildFailed").withContext(listener, "listener").isFalse();
		requireThat(listener.buildCompleted.get(), "buildCompleted").withContext(listener, "listener").isTrue();
		it.onSuccess();
	}

	@Test
	public void buildWithCacheFrom() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		DockerImage image = client.buildImage().
			export(Exporter.dockerImage().build()).
			apply(buildContext);
		requireThat(image, "image").isNotNull();

		AtomicBoolean cacheWasUsed = new AtomicBoolean(false);
		AtomicReference<Output> output = new AtomicReference<>();
		client.buildImage().cacheFrom(image.getId().getValue()).listener(new DefaultBuildListener()
		{
			@Override
			public void buildStarted(BufferedReader stdoutReader, BufferedReader stderrReader, WaitFor waitFor)
			{
				cacheWasUsed.set(false);
				output.set(null);
				super.buildStarted(stdoutReader, stderrReader, waitFor);
			}

			@Override
			public void onStderrLine(String line)
			{
				super.onStderrLine(line);
				if (line.endsWith("CACHED"))
					cacheWasUsed.set(true);
			}

			@Override
			public Output waitUntilBuildCompletes() throws IOException, InterruptedException
			{
				Output localOutput = super.waitUntilBuildCompletes();
				output.set(localOutput);
				return localOutput;
			}
		}).apply(buildContext);
		requireThat(cacheWasUsed.get(), "cacheWasUsed").withContext(output, "output").isTrue();
		it.onSuccess();
	}

	@Test(expectedExceptions = FileNotFoundException.class)
	public void buildWithDockerfileOutsideOfContextPath() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		try
		{
			client.buildImage().dockerfile(buildContext.resolve("../custom/Dockerfile")).apply(buildContext);
		}
		catch (FileNotFoundException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	@Test
	public void buildAndExportContentsToDirectory() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempDirectory = Files.createTempDirectory("");
		DockerImage image = client.buildImage().
			export(Exporter.contents(tempDirectory.toString()).directory().build()).
			apply(buildContext);
		requireThat(image, "image").isNull();

		requireThat(tempDirectory, "tempDirectory").contains(tempDirectory.resolve(FILE_IN_CONTAINER));
		it.onSuccess();
		Paths.deleteRecursively(tempDirectory);
	}

	@Test
	public void buildAndExportContentsToDirectoryMultiplePlatforms() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempDirectory = Files.createTempDirectory("");
		List<String> platforms = List.of("linux/amd64", "linux/arm64");
		DockerImageBuilder imageBuilder = client.buildImage().
			export(Exporter.contents(tempDirectory.toString()).directory().build());
		for (String platform : platforms)
			imageBuilder.platform(platform);
		DockerImage image = imageBuilder.apply(buildContext);
		requireThat(image, "image").isNull();

		List<Path> platformDirectories = new ArrayList<>(platforms.size());
		for (String platform : platforms)
			platformDirectories.add(tempDirectory.resolve(platform.replace('/', '_')));
		requireThat(tempDirectory, "tempDirectory").containsAll(platformDirectories);
		it.onSuccess();
		Paths.deleteRecursively(tempDirectory);
	}

	@Test
	public void buildAndExportContentsToTarFile() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		DockerImage image = client.buildImage().
			export(Exporter.contents(tempFile.toString()).build()).
			apply(buildContext);
		requireThat(image, "image").isNull();

		Set<String> entries = getTarEntries(tempFile.toFile());
		requireThat(entries, "entries").containsExactly(Set.of(FILE_IN_CONTAINER));
		it.onSuccess();
		Files.delete(tempFile);
	}

	@Test
	public void buildAndExportContentsToTarFileMultiplePlatforms() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		List<String> platforms = List.of("linux/amd64", "linux/arm64");
		DockerImageBuilder imageBuilder = client.buildImage().
			export(Exporter.contents(tempFile.toString()).build());
		for (String platform : platforms)
			imageBuilder.platform(platform);
		DockerImage image = imageBuilder.apply(buildContext);
		requireThat(image, "image").isNull();

		Set<String> entries = getTarEntries(tempFile.toFile());
		requireThat(entries, "entries").containsExactly(getExpectedTarEntries(List.of(FILE_IN_CONTAINER),
			platforms));
		it.onSuccess();
		Files.delete(tempFile);
	}

	/**
	 * Returns the entries that a TAR file is expected to contain.
	 *
	 * @param files     the files that each image contains
	 * @param platforms the image platforms
	 * @return the file entries
	 */
	private List<String> getExpectedTarEntries(Collection<String> files, Collection<String> platforms)
	{
		int numberOfPlatforms = platforms.size();
		List<String> result = new ArrayList<>(numberOfPlatforms + files.size() * numberOfPlatforms);
		for (String platform : platforms)
		{
			String directory = platform.replace('/', '_') + "/";
			result.add(directory);
			for (String file : files)
				result.add(directory + file);
		}
		return result;
	}

	@Test
	public void buildAndExportOciImageToDirectory() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempDirectory = Files.createTempDirectory("");
		DockerImage image = client.buildImage().
			export(Exporter.ociImage(tempDirectory.toString()).directory().build()).
			apply(buildContext);
		requireThat(image, "image").isNull();

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEmpty();

		requireThat(tempDirectory, "tempDirectory").isNotEmpty();
		it.onSuccess();
		Paths.deleteRecursively(tempDirectory);
	}

	@Test
	public void buildAndExportOciImageToDirectoryUsingDockerContainerDriver() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Builder.Id builder = client.createBuilder().
			driver(Driver.dockerContainer().build()).
			context(it.getName()).
			apply();

		Path buildContext = Path.of("src/test/resources");
		Path tempDirectory = Files.createTempDirectory("");
		DockerImage image = client.buildImage().
			export(Exporter.ociImage(tempDirectory.toString()).directory().build()).
			builder(builder).
			apply(buildContext);
		requireThat(image, "image").isNull();

		// The build driver gets loaded into the local store, but the generated image does not.
		List<DockerImage> images = client.getImages();
		DockerImage buildxImage = client.getImage("moby/buildkit:buildx-stable-1");
		requireThat(images, "images").isEqualTo(List.of(buildxImage));

		requireThat(tempDirectory, "tempDirectory").isNotEmpty();
		it.onSuccess();
		Paths.deleteRecursively(tempDirectory);
	}

	@Test
	public void buildAndExportOciImageToDirectoryMultiplePlatforms() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempDirectory = Files.createTempDirectory("");
		List<String> platforms = List.of("linux/amd64", "linux/arm64");
		DockerImageBuilder imageBuilder = client.buildImage().
			export(Exporter.ociImage(tempDirectory.toString()).directory().build());
		for (String platform : platforms)
			imageBuilder.platform(platform);
		DockerImage image = imageBuilder.apply(buildContext);
		requireThat(image, "image").isNull();

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEmpty();

		requireThat(tempDirectory, "tempDirectory").isNotEmpty();
		it.onSuccess();
		Paths.deleteRecursively(tempDirectory);
	}

	@Test
	public void buildAndExportDockerImageToTarFile() throws IOException, InterruptedException, TimeoutException
	{
		// REMINDER: Docker exporter is not capable of exporting multi-platform images to the local store.
		//
		// It outputs: "ERROR: docker exporter does not support exporting manifest lists, use the oci exporter
		// instead"
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		DockerImage image = client.buildImage().
			export(Exporter.dockerImage().path(tempFile.toString()).build()).
			apply(buildContext);
		requireThat(image, "image").isNull();

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEmpty();

		Set<String> entries = getTarEntries(tempFile.toFile());
		requireThat(entries, "entries").isNotEmpty();
		it.onSuccess();
		Paths.deleteRecursively(tempFile);
	}

	@Test
	public void buildAndExportOciImageToTarFile() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		DockerImage image = client.buildImage().
			export(Exporter.ociImage(tempFile.toString()).build()).
			apply(buildContext);
		requireThat(image, "image").isNull();

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEmpty();

		Set<String> entries = getTarEntries(tempFile.toFile());
		requireThat(entries, "entries").isNotEmpty();
		it.onSuccess();
		Files.delete(tempFile);
	}

	@Test
	public void buildAndExportOciImageToTarFileMultiplePlatforms() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		DockerImage image = client.buildImage().
			export(Exporter.ociImage(tempFile.toString()).build()).
			platform("linux/amd64").
			platform("linux/arm64").
			apply(buildContext);
		requireThat(image, "image").isNull();

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEmpty();

		Set<String> entries = getTarEntries(tempFile.toFile());
		requireThat(entries, "entries").isNotEmpty();
		it.onSuccess();
		Files.delete(tempFile);
	}

	@Test
	public void buildWithMultipleExports() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		DockerImage image = client.buildImage().
			export(Exporter.dockerImage().build()).
			export(Exporter.ociImage(tempFile.toString()).build()).
			apply(buildContext);
		requireThat(image, "image").isNotNull();

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEqualTo(List.of(image));

		Set<String> entries = getTarEntries(tempFile.toFile());
		requireThat(entries, "entries").isNotEmpty();
		it.onSuccess();
		Files.delete(tempFile);
	}

	@Test
	public void buildWithDuplicateExports() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		DockerClient client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		DockerImage image = client.buildImage().
			export(Exporter.dockerImage().build()).
			export(Exporter.dockerImage().build()).
			export(Exporter.dockerImage().build()).
			export(Exporter.dockerImage().build()).
			export(Exporter.dockerImage().build()).
			export(Exporter.ociImage(tempFile.toString()).build()).
			apply(buildContext);
		requireThat(image, "image").isNotNull();

		List<DockerImage> images = client.getImages();
		requireThat(images, "images").isEqualTo(List.of(image));

		Set<String> entries = getTarEntries(tempFile.toFile());
		requireThat(entries, "entries").isNotEmpty();
		it.onSuccess();
		Files.delete(tempFile);
	}

	/**
	 * Returns the entries of a TAR archive.
	 *
	 * @param tar the path of the TAR archive
	 * @return the archive entries
	 * @throws IOException if an error occurs while reading the file
	 */
	private Set<String> getTarEntries(File tar) throws IOException
	{
		Set<String> entries = new HashSet<>();
		try (FileInputStream is = new FileInputStream(tar);
		     TarArchiveInputStream archive = new TarArchiveInputStream(is))
		{
			while (true)
			{
				TarArchiveEntry entry = archive.getNextEntry();
				if (entry == null)
					break;
				entries.add(entry.getName());
			}
		}
		return entries;
	}
}