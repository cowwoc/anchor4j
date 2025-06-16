package io.github.cowwoc.anchor4j.docker.test.resource;

import io.github.cowwoc.anchor4j.core.internal.util.Paths;
import io.github.cowwoc.anchor4j.core.resource.BuildListener.Output;
import io.github.cowwoc.anchor4j.core.resource.BuilderCreator.Driver;
import io.github.cowwoc.anchor4j.core.resource.CoreImageBuilder.Exporter;
import io.github.cowwoc.anchor4j.core.resource.DefaultBuildListener;
import io.github.cowwoc.anchor4j.core.resource.WaitFor;
import io.github.cowwoc.anchor4j.core.test.TestBuildListener;
import io.github.cowwoc.anchor4j.docker.client.Docker;
import io.github.cowwoc.anchor4j.docker.exception.ResourceNotFoundException;
import io.github.cowwoc.anchor4j.docker.resource.DockerImage;
import io.github.cowwoc.anchor4j.docker.resource.DockerImageBuilder;
import io.github.cowwoc.anchor4j.docker.resource.ImageElement;
import io.github.cowwoc.anchor4j.docker.resource.ImageState;
import io.github.cowwoc.anchor4j.docker.test.IntegrationTestContainer;
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
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		requireThat(image, "image").isNotNull();
		it.onSuccess();
	}

	@Test
	public void alreadyPulled() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image1 = client.pullImage(EXISTING_IMAGE).pull();
		requireThat(image1, "image1").isNotNull();
		DockerImage image2 = client.pullImage(EXISTING_IMAGE).pull();
		requireThat(image1, "image1").isEqualTo(image2, "image2");
		it.onSuccess();
	}

	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void pullMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		try
		{
			client.pullImage(MISSING_IMAGE).pull();
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
		Docker client = it.getClient();
		List<ImageElement> images = client.listImages();
		requireThat(images, "images").isEmpty();
		it.onSuccess();
	}

	@Test
	public void list() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		requireThat(image, "image").isNotNull();

		List<ImageElement> images = client.listImages();
		requireThat(images, "images").size().isEqualTo(1);
		ImageElement element = images.getFirst();
		requireThat(element.referenceToTags().keySet(), "element.references()").
			isEqualTo(Set.of(EXISTING_IMAGE));
		requireThat(element.referenceToTags(), "element.referenceToTags()").
			isEqualTo(Map.of(EXISTING_IMAGE, Set.of("latest")));
		it.onSuccess();
	}

	@Test
	public void tag() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		requireThat(image, "image").isNotNull();

		List<ImageElement> images = client.listImages();
		requireThat(images, "images").size().isEqualTo(1);
		ImageElement element = images.getFirst();
		requireThat(element.referenceToTags().keySet(), "element.references()").
			isEqualTo(Set.of(EXISTING_IMAGE));

		image.tag("rocket-ship");

		images = client.listImages();
		requireThat(images, "images").size().isEqualTo(1);
		element = images.getFirst();
		requireThat(element.referenceToTags().keySet(), "element.references()").
			isEqualTo(Set.of(EXISTING_IMAGE, "rocket-ship"));
		it.onSuccess();
	}

	@Test
	public void alreadyTagged() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		requireThat(image, "image").isNotNull();

		List<ImageElement> images = client.listImages();
		requireThat(images, "images").size().isEqualTo(1);
		ImageElement element = images.getFirst();
		requireThat(element.referenceToTags().keySet(), "element.references()").
			isEqualTo(Set.of(EXISTING_IMAGE));

		image.tag("rocket-ship");
		image.tag("rocket-ship");

		images = client.listImages();
		requireThat(images, "images").size().isEqualTo(1);
		element = images.getFirst();
		requireThat(element.referenceToTags().keySet(), "element.references()").
			isEqualTo(Set.of(EXISTING_IMAGE, "rocket-ship"));
		it.onSuccess();
	}

	@Test(expectedExceptions = ResourceNotFoundException.class)
	public void tagMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		requireThat(image, "image").isNotNull();

		image.remover().remove();
		try
		{
			image.tag("rocket-ship");
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
		Docker client = it.getClient();
		DockerImage image = client.pullImage(EXISTING_IMAGE).pull();
		ImageState state = client.getImageState(EXISTING_IMAGE);
		requireThat(image.getId(), "image.getId()").isEqualTo(state.getId(), "state.getId()");
		it.onSuccess();
	}

	@Test
	public void getMissing() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		ImageState image = client.getImageState(MISSING_IMAGE);
		requireThat(image, "missingImage").isNull();
		it.onSuccess();
	}

	@Test
	public void buildAndExportToDocker() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		DockerImage image = client.buildImage().export(Exporter.dockerImage().build()).build(buildContext);
		List<ImageElement> images = client.listImages();
		requireThat(images, "images").contains(new ImageElement(image, Map.of(), Map.of()));
		it.onSuccess();
	}

	@Test
	public void buildWithCustomDockerfile() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		DockerImage image = client.buildImage().dockerfile(buildContext.resolve("custom/Dockerfile")).
			export(Exporter.dockerImage().build()).
			build(buildContext);
		List<ImageElement> images = client.listImages();
		requireThat(images, "images").contains(new ImageElement(image, Map.of(), Map.of()));
		it.onSuccess();
	}

	@Test(expectedExceptions = FileNotFoundException.class)
	public void buildWithMissingDockerfile() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		try
		{
			client.buildImage().dockerfile(buildContext.resolve("missing/Dockerfile")).build(buildContext);
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
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		DockerImage image = client.buildImage().platform("linux/amd64").
			export(Exporter.dockerImage().build()).
			build(buildContext);
		List<ImageElement> images = client.listImages();
		requireThat(images, "images").contains(new ImageElement(image, Map.of(), Map.of()));
		it.onSuccess();
	}

	@Test
	public void buildWithMultiplePlatform() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		DockerImage image = client.buildImage().platform("linux/amd64").platform("linux/arm64").
			export(Exporter.dockerImage().build()).
			build(buildContext);
		List<ImageElement> images = client.listImages();
		requireThat(images, "images").contains(new ImageElement(image, Map.of(), Map.of()));
		it.onSuccess();
	}

	@Test
	public void buildWithTag() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		DockerImage image = client.buildImage().reference("integration-test").export(
				Exporter.dockerImage().build()).
			build(buildContext);
		ImageState state = image.getState();
		List<ImageElement> images = client.listImages();
		requireThat(images, "images").contains(new ImageElement(
			image, Map.of("integration-test", Set.of("latest")), state.referenceToDigest()));
		it.onSuccess();
	}

	@Test
	public void buildPassedWithCustomListener() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		TestBuildListener listener = new TestBuildListener();
		client.buildImage().listener(listener).build(buildContext);
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
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		DockerImage image = client.buildImage().
			export(Exporter.ociImage(tempFile.toString()).build()).
			build(buildContext);
		requireThat(image, "id").isNotNull();

		AtomicBoolean cacheWasUsed = new AtomicBoolean(false);
		AtomicReference<Output> output = new AtomicReference<>();
		client.buildImage().cacheFrom(image.getId()).listener(new DefaultBuildListener()
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
		}).build(buildContext);
		requireThat(cacheWasUsed.get(), "cacheWasUsed").withContext(output, "output").isTrue();
		Files.delete(tempFile);
		it.onSuccess();
	}

	@Test(expectedExceptions = FileNotFoundException.class)
	public void buildWithDockerfileOutsideOfContextPath() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");
		try
		{
			client.buildImage().dockerfile(buildContext.resolve("../custom/Dockerfile")).build(buildContext);
		}
		catch (FileNotFoundException e)
		{
			it.onSuccess();
			throw e;
		}
	}

	@Test
	public void buildAndOutputContentsToDirectory() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempDirectory = Files.createTempDirectory("");
		DockerImage image = client.buildImage().
			export(Exporter.contents(tempDirectory.toString()).directory().build()).
			build(buildContext);
		requireThat(image, "id").isNull();

		requireThat(tempDirectory, "tempDirectory").contains(tempDirectory.resolve(FILE_IN_CONTAINER));
		it.onSuccess();
		Paths.deleteRecursively(tempDirectory);
	}

	@Test
	public void buildAndOutputContentsToDirectoryMultiplePlatforms() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempDirectory = Files.createTempDirectory("");
		List<String> platforms = List.of("linux/amd64", "linux/arm64");
		DockerImageBuilder imageBuilder = client.buildImage().
			export(Exporter.contents(tempDirectory.toString()).directory().build());
		for (String platform : platforms)
			imageBuilder.platform(platform);
		DockerImage image = imageBuilder.build(buildContext);
		requireThat(image, "image").isNull();

		List<Path> platformDirectories = new ArrayList<>(platforms.size());
		for (String platform : platforms)
			platformDirectories.add(tempDirectory.resolve(platform.replace('/', '_')));
		requireThat(tempDirectory, "tempDirectory").containsAll(platformDirectories);
		it.onSuccess();
		Paths.deleteRecursively(tempDirectory);
	}

	@Test
	public void buildAndOutputContentsToTarFile() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		DockerImage image = client.buildImage().
			export(Exporter.contents(tempFile.toString()).build()).
			build(buildContext);
		requireThat(image, "id").isNull();

		Set<String> entries = getTarEntries(tempFile.toFile());
		requireThat(entries, "entries").containsExactly(Set.of(FILE_IN_CONTAINER));
		it.onSuccess();
		Files.delete(tempFile);
	}

	@Test
	public void buildAndOutputContentsToTarFileMultiplePlatforms() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		List<String> platforms = List.of("linux/amd64", "linux/arm64");
		DockerImageBuilder imageBuilder = client.buildImage().
			export(Exporter.contents(tempFile.toString()).build());
		for (String platform : platforms)
			imageBuilder.platform(platform);
		DockerImage image = imageBuilder.build(buildContext);
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
	public void buildAndOutputOciImageToDirectory() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempDirectory = Files.createTempDirectory("");
		DockerImage image = client.buildImage().
			export(Exporter.ociImage(tempDirectory.toString()).directory().build()).
			build(buildContext);
		requireThat(image, "image").isNotNull();
		List<ImageElement> images = client.listImages();
		requireThat(images, "images").isEmpty();
		requireThat(tempDirectory, "tempDirectory").isNotEmpty();
		it.onSuccess();
		Paths.deleteRecursively(tempDirectory);
	}

	@Test
	public void buildAndOutputOciImageToDirectoryUsingDockerContainerDriver() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		String builder = client.createBuilder().driver(Driver.dockerContainer().build()).
			context(it.getName()).
			create();

		Path buildContext = Path.of("src/test/resources");

		Path tempDirectory = Files.createTempDirectory("");
		DockerImage image = client.buildImage().
			export(Exporter.ociImage(tempDirectory.toString()).directory().build()).
			builder(builder).
			build(buildContext);
		requireThat(image, "image").isNotNull();

		List<ImageElement> images = client.listImages();
		requireThat(images, "images").isNotEmpty();
		requireThat(tempDirectory, "tempDirectory").isNotEmpty();
		it.onSuccess();
		Paths.deleteRecursively(tempDirectory);
	}

	@Test
	public void buildAndOutputOciImageToDirectoryMultiplePlatforms() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempDirectory = Files.createTempDirectory("");
		List<String> platforms = List.of("linux/amd64", "linux/arm64");
		DockerImageBuilder imageBuilder = client.buildImage().
			export(Exporter.ociImage(tempDirectory.toString()).directory().build());
		for (String platform : platforms)
			imageBuilder.platform(platform);
		DockerImage image = imageBuilder.build(buildContext);
		requireThat(image, "image").isNotNull();
		List<ImageElement> images = client.listImages();
		requireThat(images, "images").isEmpty();
		requireThat(tempDirectory, "tempDirectory").isNotEmpty();
		it.onSuccess();
		Paths.deleteRecursively(tempDirectory);
	}

	@Test
	public void buildAndOutputDockerImageToTarFile() throws IOException, InterruptedException, TimeoutException
	{
		// REMINDER: Docker exporter is not capable of exporting multi-platform images to the local store.
		//
		// It outputs: "ERROR: docker exporter does not support exporting manifest lists, use the oci exporter
		// instead"
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		DockerImage image = client.buildImage().
			export(Exporter.dockerImage().path(tempFile.toString()).build()).
			build(buildContext);
		requireThat(image, "image").isNotNull();
		List<ImageElement> images = client.listImages();
		requireThat(images, "images").isEmpty();
		Set<String> entries = getTarEntries(tempFile.toFile());
		requireThat(entries, "entries").isNotEmpty();
		it.onSuccess();
		Paths.deleteRecursively(tempFile);
	}

	@Test
	public void buildAndOutputOciImageToTarFile() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		DockerImage image = client.buildImage().
			export(Exporter.ociImage(tempFile.toString()).build()).
			build(buildContext);
		requireThat(image, "image").isNotNull();
		List<ImageElement> images = client.listImages();
		requireThat(images, "images").isEmpty();

		Set<String> entries = getTarEntries(tempFile.toFile());
		requireThat(entries, "entries").isNotEmpty();
		it.onSuccess();
		Files.delete(tempFile);
	}

	@Test
	public void buildAndOutputOciImageToTarFileMultiplePlatforms() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile = Files.createTempFile("", ".tar");
		DockerImage image = client.buildImage().
			export(Exporter.ociImage(tempFile.toString()).build()).
			platform("linux/amd64").
			platform("linux/arm64").
			build(buildContext);
		requireThat(image, "image").isNotNull();
		List<ImageElement> images = client.listImages();
		requireThat(images, "images").isEmpty();

		Set<String> entries = getTarEntries(tempFile.toFile());
		requireThat(entries, "entries").isNotEmpty();
		it.onSuccess();
		Files.delete(tempFile);
	}

	@Test
	public void buildWithMultipleOutputs() throws IOException, InterruptedException, TimeoutException
	{
		IntegrationTestContainer it = new IntegrationTestContainer();
		Docker client = it.getClient();
		Path buildContext = Path.of("src/test/resources");

		Path tempFile1 = Files.createTempFile("", ".tar");
		Path tempFile2 = Files.createTempFile("", ".tar");
		DockerImage image = client.buildImage().
			export(Exporter.dockerImage().path(tempFile1.toString()).build()).
			export(Exporter.ociImage(tempFile2.toString()).build()).
			build(buildContext);
		requireThat(image, "image").isNotNull();
		List<ImageElement> images = client.listImages();
		requireThat(images, "images").isEmpty();

		Set<String> entries1 = getTarEntries(tempFile1.toFile());
		requireThat(entries1, "entries1").isNotEmpty();

		Set<String> entries2 = getTarEntries(tempFile1.toFile());
		requireThat(entries2, "entries2").isNotEmpty();
		it.onSuccess();
		Files.delete(tempFile1);
		Files.delete(tempFile2);
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