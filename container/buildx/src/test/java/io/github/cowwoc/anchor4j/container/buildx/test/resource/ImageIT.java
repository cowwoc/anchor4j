package io.github.cowwoc.anchor4j.container.buildx.test.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.cowwoc.anchor4j.buildx.client.BuildXClient;
import io.github.cowwoc.anchor4j.container.core.resource.Builder;
import io.github.cowwoc.anchor4j.container.core.resource.BuilderCreator.Driver;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImage;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImageBuilder;
import io.github.cowwoc.anchor4j.container.core.resource.ContainerImageBuilder.Exporter;
import io.github.cowwoc.anchor4j.container.core.resource.DefaultBuildListener;
import io.github.cowwoc.anchor4j.container.core.resource.WaitFor;
import io.github.cowwoc.anchor4j.container.core.resource.test.TestBuildListener;
import io.github.cowwoc.anchor4j.core.internal.client.InternalClient;
import io.github.cowwoc.anchor4j.core.internal.util.Paths;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.EOFException;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

public final class ImageIT
{
	static final String FILE_IN_CONTAINER = "logback-test.xml";

	@Test
	public void build() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempFile = Files.createTempFile("", ".tar");
			ContainerImage image = client.buildImage().
				export(Exporter.ociImage(tempFile.toString()).build()).
				apply(buildContext);
			requireThat(image, "image").isNull();

			Set<String> entries = getTarEntries(tempFile.toFile());
			requireThat(entries, "entries").isNotEmpty();
			Files.delete(tempFile);
		}
	}

	@Test
	public void buildWithCustomDockerfile() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempFile = Files.createTempFile("", ".tar");
			ContainerImage image = client.buildImage().
				dockerfile(buildContext.resolve("custom/Dockerfile")).
				export(Exporter.ociImage(tempFile.toString()).build()).
				apply(buildContext);
			requireThat(image, "image").isNull();

			Set<String> entries = getTarEntries(tempFile.toFile());
			requireThat(entries, "entries").isNotEmpty();
			Files.delete(tempFile);
		}
	}

	@Test(expectedExceptions = FileNotFoundException.class)
	public void buildWithMissingDockerfile() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempFile = Files.createTempFile("", ".tar");
			try
			{
				client.buildImage().dockerfile(buildContext.resolve("missing/Dockerfile")).apply(buildContext);
			}
			catch (FileNotFoundException e)
			{
				Files.delete(tempFile);
				throw e;
			}
		}
	}

	@Test
	public void buildWithSinglePlatform() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempFile = Files.createTempFile("", ".tar");
			ContainerImage image = client.buildImage().
				platform("linux/amd64").
				export(Exporter.ociImage(tempFile.toString()).build()).
				apply(buildContext);
			requireThat(image, "image").isNull();

			Set<String> entries = getTarEntries(tempFile.toFile());
			requireThat(entries, "entries").isNotEmpty();
			Files.delete(tempFile);
		}
	}

	@Test
	public void buildWithMultiplePlatform() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempFile = Files.createTempFile("", ".tar");
			ContainerImage image = client.buildImage().
				platform("linux/amd64").
				platform("linux/arm64").
				export(Exporter.ociImage(tempFile.toString()).build()).
				apply(buildContext);
			requireThat(image, "image").isNull();

			Set<String> entries = getTarEntries(tempFile.toFile());
			requireThat(entries, "entries").isNotEmpty();
			Files.delete(tempFile);
		}
	}

	@Test
	public void buildWithReference() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempFile = Files.createTempFile("", ".tar");
			String expected = "docker.io/library/integration-test:latest";
			ContainerImage image = client.buildImage().
				reference(expected).
				export(Exporter.ociImage(tempFile.toString()).build()).
				apply(buildContext);
			requireThat(image, "image").isNull();

			JsonNode indexJson = getIndexJson(client, tempFile.toFile());
			assert indexJson != null;
			String actual = indexJson.get("manifests").get(0).get("annotations").get("io.containerd.image.name").
				asText();
			requireThat(actual, "actual").withContext(tempFile, "tempFile").
				isEqualTo(expected, "expected");
			Files.delete(tempFile);
		}
	}

	@Test
	public void buildPassedWithCustomListener() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			TestBuildListener listener = new TestBuildListener();
			client.buildImage().listener(listener).apply(buildContext);
			requireThat(listener.buildStarted.get(), "buildStarted").isTrue();
			requireThat(listener.waitUntilBuildCompletes.get(), "waitUntilBuildCompletes").isTrue();
			requireThat(listener.buildPassed.get(), "buildSucceeded").isTrue();
			requireThat(listener.buildFailed.get(), "buildFailed").isFalse();
			requireThat(listener.buildCompleted.get(), "buildCompleted").isTrue();
		}
	}

	@Test
	public void buildFailedWithCustomListener() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			TestBuildListener listener = new TestBuildListener();
			try
			{
				client.buildImage().listener(listener).dockerfile(buildContext.resolve("missing/Dockerfile")).
					apply(buildContext);
			}
			catch (FileNotFoundException _)
			{
				requireThat(listener.buildStarted.get(), "buildStarted").isTrue();
				requireThat(listener.waitUntilBuildCompletes.get(), "waitUntilBuildCompletes").isTrue();
				requireThat(listener.buildPassed.get(), "buildSucceeded").isFalse();
				requireThat(listener.buildFailed.get(), "buildFailed").isTrue();
				requireThat(listener.buildCompleted.get(), "buildCompleted").isTrue();
			}
		}
	}

	@Test
	public void buildWithCacheFrom() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			ContainerImage image = client.buildImage().
				export(Exporter.dockerImage().build()).
				apply(buildContext);
			requireThat(image, "image").isNotNull();

			AtomicBoolean cacheWasUsed = new AtomicBoolean(false);
			client.buildImage().cacheFrom("type=local,src=" + image.getId().getValue()).
				listener(new DefaultBuildListener()
				{
					@Override
					public void buildStarted(BufferedReader stdoutReader, BufferedReader stderrReader, WaitFor waitFor)
					{
						cacheWasUsed.set(false);
						super.buildStarted(stdoutReader, stderrReader, waitFor);
					}

					@Override
					public void onStderrLine(String line)
					{
						super.onStderrLine(line);
						if (line.endsWith("CACHED"))
							cacheWasUsed.set(true);
					}
				}).apply(buildContext);
			requireThat(cacheWasUsed.get(), "cacheWasUsed").isTrue();
		}
	}

	@Test(expectedExceptions = FileNotFoundException.class)
	public void buildWithDockerfileOutsideOfContextPath() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");
			client.buildImage().dockerfile(buildContext.resolve("../custom/Dockerfile")).apply(buildContext);
		}
	}

	@Test
	public void buildAndOutputContentsToDirectory() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempDirectory = Files.createTempDirectory("");
			ContainerImage image = client.buildImage().
				export(Exporter.contents(tempDirectory.toString()).directory().build()).
				apply(buildContext);
			requireThat(image, "image").isNull();

			requireThat(tempDirectory, "tempDirectory").contains(tempDirectory.resolve(FILE_IN_CONTAINER));
			Paths.deleteRecursively(tempDirectory);
		}
	}

	@Test
	public void buildAndOutputContentsToDirectoryMultiplePlatforms() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempDirectory = Files.createTempDirectory("");
			List<String> platforms = List.of("linux/amd64", "linux/arm64");
			ContainerImageBuilder imageBuilder = client.buildImage().
				export(Exporter.contents(tempDirectory.toString()).directory().build());
			for (String platform : platforms)
				imageBuilder.platform(platform);
			ContainerImage id = imageBuilder.apply(buildContext);
			requireThat(id, "id").isNull();

			List<Path> platformDirectories = new ArrayList<>(platforms.size());
			for (String platform : platforms)
				platformDirectories.add(tempDirectory.resolve(platform.replace('/', '_')));
			requireThat(tempDirectory, "tempDirectory").containsAll(platformDirectories);
			Paths.deleteRecursively(tempDirectory);
		}
	}

	@Test
	public void buildAndOutputContentsToTarFile() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempFile = Files.createTempFile("", ".tar");
			ContainerImage image = client.buildImage().
				export(Exporter.contents(tempFile.toString()).build()).
				apply(buildContext);
			requireThat(image, "image").isNull();

			Set<String> entries = getTarEntries(tempFile.toFile());
			requireThat(entries, "entries").containsExactly(Set.of(FILE_IN_CONTAINER));
			Files.delete(tempFile);
		}
	}

	@Test
	public void buildAndOutputContentsToTarFileMultiplePlatforms() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempFile = Files.createTempFile("", ".tar");
			List<String> platforms = List.of("linux/amd64", "linux/arm64");
			ContainerImageBuilder imageBuilder = client.buildImage().
				export(Exporter.contents(tempFile.toString()).build());
			for (String platform : platforms)
				imageBuilder.platform(platform);
			ContainerImage image = imageBuilder.apply(buildContext);
			requireThat(image, "image").isNull();

			Set<String> entries = getTarEntries(tempFile.toFile());
			requireThat(entries, "entries").containsExactly(getExpectedTarEntries(List.of(FILE_IN_CONTAINER),
				platforms));
			Files.delete(tempFile);
		}
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
	public void buildAndOutputOciImageToDirectory() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempDirectory = Files.createTempDirectory("");
			ContainerImage image = client.buildImage().
				export(Exporter.ociImage(tempDirectory.toString()).directory().build()).
				apply(buildContext);
			requireThat(image, "image").isNull();

			requireThat(tempDirectory, "tempDirectory").isNotEmpty();
			Paths.deleteRecursively(tempDirectory);
		}
	}

	@Test
	public void buildAndOutputOciImageToDirectoryUsingDockerContainerDriver() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Builder.Id builder = client.createBuilder().driver(Driver.dockerContainer().build()).apply();

			Path buildContext = Path.of("src/test/resources");

			Path tempDirectory = Files.createTempDirectory("");
			ContainerImage image = client.buildImage().
				export(Exporter.ociImage(tempDirectory.toString()).directory().build()).
				builder(builder).
				apply(buildContext);
			requireThat(image, "image").isNull();

			requireThat(tempDirectory, "tempDirectory").isNotEmpty();
			Paths.deleteRecursively(tempDirectory);
		}
	}

	@Test
	public void buildAndOutputOciImageToDirectoryMultiplePlatforms() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempDirectory = Files.createTempDirectory("");
			List<String> platforms = List.of("linux/amd64", "linux/arm64");
			ContainerImageBuilder imageBuilder = client.buildImage().
				export(Exporter.ociImage(tempDirectory.toString()).directory().build());
			for (String platform : platforms)
				imageBuilder.platform(platform);
			ContainerImage image = imageBuilder.apply(buildContext);
			requireThat(image, "image").isNull();

			requireThat(tempDirectory, "tempDirectory").isNotEmpty();
			Paths.deleteRecursively(tempDirectory);
		}
	}

	@Test
	public void buildAndOutputDockerImageToTarFile() throws IOException, InterruptedException
	{
		// REMINDER: Docker exporter is not capable of exporting multi-platform images to the local store.
		//
		// It outputs: "ERROR: docker exporter does not support exporting manifest lists, use the oci exporter
		// instead"
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempFile = Files.createTempFile("", ".tar");
			ContainerImage image = client.buildImage().
				export(Exporter.dockerImage().path(tempFile.toString()).build()).
				apply(buildContext);
			requireThat(image, "image").isNull();

			Set<String> entries = getTarEntries(tempFile.toFile());
			requireThat(entries, "entries").isNotEmpty();
			Paths.deleteRecursively(tempFile);
		}
	}

	@Test
	public void buildAndOutputOciImageToTarFile() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempFile = Files.createTempFile("", ".tar");
			ContainerImage image = client.buildImage().
				export(Exporter.ociImage(tempFile.toString()).build()).
				apply(buildContext);
			requireThat(image, "image").isNull();

			Set<String> entries = getTarEntries(tempFile.toFile());
			requireThat(entries, "entries").isNotEmpty();
			Files.delete(tempFile);
		}
	}

	@Test
	public void buildAndOutputOciImageToTarFileMultiplePlatforms() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempFile = Files.createTempFile("", ".tar");
			ContainerImage image = client.buildImage().
				export(Exporter.ociImage(tempFile.toString()).build()).
				platform("linux/amd64").
				platform("linux/arm64").
				apply(buildContext);
			requireThat(image, "image").isNull();

			Set<String> entries = getTarEntries(tempFile.toFile());
			requireThat(entries, "entries").isNotEmpty();
			Files.delete(tempFile);
		}
	}

	@Test
	public void buildWithMultipleOutputs() throws IOException, InterruptedException
	{
		try (BuildXClient client = BuildXClient.build())
		{
			Path buildContext = Path.of("src/test/resources");

			Path tempFile1 = Files.createTempFile("", ".tar");
			Path tempFile2 = Files.createTempFile("", ".tar");
			ContainerImage image = client.buildImage().
				export(Exporter.dockerImage().path(tempFile1.toString()).build()).
				export(Exporter.ociImage(tempFile2.toString()).build()).
				apply(buildContext);
			requireThat(image, "image").isNull();

			Set<String> entries1 = getTarEntries(tempFile1.toFile());
			requireThat(entries1, "entries1").isNotEmpty();

			Set<String> entries2 = getTarEntries(tempFile2.toFile());
			requireThat(entries2, "entries2").isNotEmpty();
			Files.delete(tempFile1);
			Files.delete(tempFile2);
		}
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

	/**
	 * Returns the JSON inside {@code index.json} inside a TAR archive.
	 *
	 * @param client the client configuration
	 * @param tar    the path of the TAR archive
	 * @return the JSON
	 * @throws IOException if an error occurs while reading the file
	 */
	private JsonNode getIndexJson(BuildXClient client, File tar) throws IOException
	{
		try (FileInputStream is = new FileInputStream(tar);
		     TarArchiveInputStream archive = new TarArchiveInputStream(is))
		{
			while (true)
			{
				TarArchiveEntry entry = archive.getNextEntry();
				if (entry == null)
					break;
				if (entry.getName().equals("index.json"))
				{
					InternalClient internalClient = (InternalClient) client;
					JsonMapper jm = internalClient.getJsonMapper();
					int size = Math.toIntExact(entry.getSize());
					byte[] buffer = new byte[size];
					int offset = 0;
					while (offset < buffer.length)
					{
						int count = archive.readNBytes(buffer, offset, size - offset);
						if (count == 0)
							throw new EOFException();
						offset += count;
					}
					return jm.readTree(buffer);
				}
			}
		}
		return null;
	}
}