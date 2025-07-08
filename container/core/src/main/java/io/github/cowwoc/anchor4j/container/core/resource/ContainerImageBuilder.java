package io.github.cowwoc.anchor4j.container.core.resource;

import io.github.cowwoc.anchor4j.container.core.exception.ContextNotFoundException;
import io.github.cowwoc.anchor4j.container.core.exception.UnsupportedExporterException;
import io.github.cowwoc.anchor4j.container.core.internal.resource.DefaultContainerImageBuilder.AbstractExporter;
import io.github.cowwoc.anchor4j.container.core.internal.resource.DefaultContainerImageBuilder.DefaultContentsExporterBuilder;
import io.github.cowwoc.anchor4j.container.core.internal.resource.DefaultContainerImageBuilder.DefaultDockerImageExporterBuilder;
import io.github.cowwoc.anchor4j.container.core.internal.resource.DefaultContainerImageBuilder.DefaultOciImageExporterBuilder;
import io.github.cowwoc.anchor4j.container.core.internal.resource.DefaultContainerImageBuilder.DefaultRegistryExporterBuilder;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Represents an operation that builds an image.
 */
public interface ContainerImageBuilder
{
	/**
	 * Sets the path of the {@code Dockerfile}. By default, the builder looks for the file in the current
	 * working directory.
	 *
	 * @param dockerfile the path of the {@code Dockerfile}
	 * @return this
	 * @throws NullPointerException if {@code dockerFile} is null
	 */
	ContainerImageBuilder dockerfile(Path dockerfile);

	/**
	 * Adds a platform to build the image for.
	 *
	 * @param platform the platform of the image
	 * @return this
	 * @throws NullPointerException     if {@code platform} is null
	 * @throws IllegalArgumentException if {@code platform} contains whitespace or is empty
	 */
	ContainerImageBuilder platform(String platform);

	/**
	 * Adds a reference to apply to the image.
	 *
	 * @param reference the reference
	 * @return this
	 * @throws NullPointerException     if {@code reference} is null
	 * @throws IllegalArgumentException if {@code reference}:
	 *                                  <ul>
	 *                                    <li>is empty.</li>
	 *                                    <li>contains any character other than lowercase letters (a–z),
	 *                                    digits (0–9) and the following characters: {@code '.'}, {@code '/'},
	 *                                    {@code ':'}, {@code '_'}, {@code '-'}, {@code '@'}.</li>
	 *                                  </ul>
	 */
	ContainerImageBuilder reference(String reference);

	/**
	 * Adds an external cache source to use. By default, no external cache sources are used.
	 *
	 * @param source the external cache source
	 * @return this
	 * @throws IllegalArgumentException if {@code source} contains whitespace, or is empty
	 * @see <a href="https://docs.docker.com/reference/cli/docker/buildx/build/#cache-from">Possible values</a>
	 */
	ContainerImageBuilder cacheFrom(String source);

	/**
	 * Adds an output format and location for the image. By default, a build has no exporters, meaning the
	 * resulting image is discarded after the build completes. However, multiple exporters can be configured to
	 * export the image to one or more destinations.
	 * <p>
	 * If an equivalent exporter has already been added, this method has no effect.
	 *
	 * @param exporter the exporter
	 * @return this
	 * @throws NullPointerException if {@code exporter} is null
	 */
	ContainerImageBuilder export(Exporter exporter);

	/**
	 * Sets the builder instance to use for building the image.
	 *
	 * @param builder the name of the builder. The value must start with a letter, or digit, or underscore, and
	 *                may be followed by additional characters consisting of letters, digits, underscores,
	 *                periods or hyphens.
	 * @return this
	 * @throws NullPointerException     if {@code builder} is null
	 * @throws IllegalArgumentException if {@code builder}'s format is invalid
	 */
	ContainerImageBuilder builder(String builder);

	/**
	 * Adds a build listener.
	 *
	 * @param listener the build listener
	 * @return this
	 * @throws NullPointerException if {@code listener} is null
	 */
	ContainerImageBuilder listener(BuildListener listener);

	/**
	 * Builds the image.
	 * <p>
	 * <strong>Warning:</strong> This method does <em>not</em> export the built image by default.
	 * To specify and trigger export behavior, you must explicitly call {@link #export(Exporter)}.
	 *
	 * @param buildContext the build context, the directory relative to which paths in the Dockerfile are
	 *                     evaluated
	 * @return the new image, or null if none of the {@link #export(Exporter) exports} output an image
	 * @throws NullPointerException         if {@code buildContext} is null
	 * @throws IllegalArgumentException     if {@code buildContext} is not a valid {@code Path}
	 * @throws FileNotFoundException        if a referenced path does not exist
	 * @throws UnsupportedExporterException if the builder does not support one of the requested exporters
	 * @throws ContextNotFoundException     if the Docker context cannot be found or resolved
	 * @throws IOException                  if an I/O error occurs. These errors are typically transient, and
	 *                                      retrying the request may resolve the issue.
	 * @throws InterruptedException         if the thread is interrupted before the operation completes. This
	 *                                      can happen due to shutdown signals.
	 */
	ContainerImage apply(String buildContext) throws IOException, InterruptedException;

	/**
	 * Builds the image.
	 * <p>
	 * <strong>Warning:</strong> This method does <em>not</em> export the built image by default.
	 * To specify and trigger export behavior, you must explicitly call {@link #export(Exporter)}.
	 *
	 * @param buildContext the build context, the directory relative to which paths in the Dockerfile are
	 *                     evaluated
	 * @return the new image, or null if none of the {@link #export(Exporter) exports} output an image
	 * @throws NullPointerException         if {@code buildContext} is null
	 * @throws FileNotFoundException        if a referenced path does not exist
	 * @throws UnsupportedExporterException if the builder does not support one of the requested exporters
	 * @throws ContextNotFoundException     if the Docker context cannot be found or resolved
	 * @throws IOException                  if an I/O error occurs. These errors are typically transient, and
	 *                                      retrying the request may resolve the issue.
	 * @throws InterruptedException         if the thread is interrupted before the operation completes. This
	 *                                      can happen due to shutdown signals.
	 */
	ContainerImage apply(Path buildContext) throws IOException, InterruptedException;

	/**
	 * The type of encoding used by progress output.
	 */
	enum ProgressType
	{
		/**
		 * Output the build progress using ANSI control sequences for colors and to redraw lines.
		 */
		TTY,
		/**
		 * Output the build progress using a plain text format.
		 */
		PLAIN,
		/**
		 * Suppress the build output and print the image ID on success.
		 */
		QUIET,
		/**
		 * Output the build progress as <a href="https://jsonlines.org/">JSON lines</a>.
		 */
		RAW_JSON;

		/**
		 * Returns the command-line representation of this option.
		 *
		 * @return the command-line value
		 */
		public String toCommandLine()
		{
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * Transforms or transmits the build output.
	 */
	sealed interface Exporter permits AbstractExporter
	{
		/**
		 * Outputs the contents of the resulting image.
		 * <p>
		 * For multi-platform builds, a separate subdirectory will be created for each platform.
		 * <p>
		 * For example, the directory structure might look like:
		 * <pre>{@code
		 * /
		 * ├── linux_amd64/
		 * └── linux_arm64/
		 * }</pre>
		 *
		 * @param path the output location, which is either a TAR archive or a directory depending on whether
		 *             {@link ContentsExporterBuilder#directory() directory()} is invoked
		 * @return the exporter
		 * @throws NullPointerException     if {@code path} is null
		 * @throws IllegalArgumentException if {@code path} contains whitespace or is empty
		 */
		@CheckReturnValue
		static ContentsExporterBuilder contents(String path)
		{
			return new DefaultContentsExporterBuilder(path);
		}

		/**
		 * Outputs the resulting image in Docker container format.
		 *
		 * @return the exporter
		 */
		@CheckReturnValue
		static DockerImageExporterBuilder dockerImage()
		{
			return new DefaultDockerImageExporterBuilder();
		}

		/**
		 * Outputs images to disk in the
		 * <a href="https://github.com/opencontainers/image-spec/blob/main/image-layout.md">OCI container
		 * format</a>.
		 * <p>
		 * For multi-platform builds, a separate subdirectory will be created for each platform.
		 * <p>
		 * For example, the directory structure might look like:
		 * <pre>{@code
		 * /
		 * ├── linux_amd64/
		 * └── linux_arm64/
		 * }</pre>
		 *
		 * @param path the output location, which is either a TAR archive or a directory depending on whether
		 *             {@link OciImageExporterBuilder#directory() directory()} is invoked
		 * @return the exporter
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code path} contains whitespace or is empty
		 */
		@CheckReturnValue
		static OciImageExporterBuilder ociImage(String path)
		{
			return new DefaultOciImageExporterBuilder(path);
		}

		/**
		 * Pushes the resulting image to a registry.
		 *
		 * @return the exporter
		 */
		@CheckReturnValue
		static RegistryExporterBuilder registry()
		{
			return new DefaultRegistryExporterBuilder();
		}
	}

	/**
	 * Builds an exporter that outputs the contents of images to disk.
	 */
	interface ContentsExporterBuilder
	{
		/**
		 * Specifies that the image files should be written to a directory. By default, the image is packaged as a
		 * TAR archive, with {@code path} representing the archive’s location. When this method is used,
		 * {@code path} is treated as a directory, and image files are written directly into it.
		 *
		 * @return this
		 */
		ContentsExporterBuilder directory();

		/**
		 * Builds the exporter.
		 *
		 * @return the exporter
		 */
		Exporter build();
	}

	/**
	 * Builds an exporter that outputs images.
	 */
	interface ImageExporterBuilder
	{
		/**
		 * Sets the image reference of this output. By default, the output name is derived from the image's tag,
		 * if specified; otherwise, the output remains unnamed.
		 *
		 * @param name the image reference
		 * @return this
		 * @throws NullPointerException     if {@code name} is null
		 * @throws IllegalArgumentException if {@code name} contains whitespace or is empty
		 */
		ImageExporterBuilder name(String name);

		/**
		 * Sets the compression type used by the output.
		 * <p>
		 * While the default values provide a good out-of-the-box experience, you may wish to tweak the parameters
		 * to optimize for storage vs compute costs.
		 * <p>
		 * Both Docker and OCI formats compress the image layers. Additionally, when outputting to a TAR archive,
		 * the OCI format supports compressing the entire TAR archive.
		 *
		 * @param type the type
		 * @return this
		 * @throws NullPointerException if {@code type} is null
		 */
		ImageExporterBuilder compressionType(CompressionType type);

		/**
		 * Sets the compression level used by the output.
		 * <p>
		 * As a general rule, the higher the number, the smaller the resulting file will be, and the longer the
		 * compression will take to run.
		 * <p>
		 * Valid compression level ranges depend on the selected {@code compressionType}:
		 * <ul>
		 *   <li>{@code gzip} and {@code estargz}: level must be between {@code 0} and {@code 9}.</li>
		 *   <li>{@code zstd}: level must be between {@code 0} and {@code 22}.</li>
		 * </ul>
		 * If {@code compressionType} is {@code uncompressed} then {@code compressionLevel} has no effect.
		 *
		 * @param compressionLevel the compression level, increasing the compression effort as the level
		 *                         increases
		 * @return this
		 * @throws IllegalArgumentException if {@code compressionLevel} is out of range
		 */
		ImageExporterBuilder compressionLevel(int compressionLevel);

		/**
		 * Builds the exporter.
		 *
		 * @return the exporter
		 */
		Exporter build();
	}

	/**
	 * Builds an exporter that outputs images using the Docker container format.
	 */
	interface DockerImageExporterBuilder extends ImageExporterBuilder
	{
		@Override
		DockerImageExporterBuilder name(String name);

		@Override
		DockerImageExporterBuilder compressionType(CompressionType type);

		@Override
		DockerImageExporterBuilder compressionLevel(int compressionLevel);

		/**
		 * Indicates that the image should be exported to disk as a TAR archive, rather than being loaded into the
		 * Docker image store (which is the default behavior).
		 * <p>
		 * For multi-platform builds, the TAR archive will contain a separate subdirectory for each target
		 * platform.
		 * <p>
		 * For example, the directory structure might look like:
		 * <pre>{@code
		 * /
		 * ├── linux_amd64/
		 * └── linux_arm64/
		 * }</pre>
		 *
		 * @param path the path of the TAR archive
		 * @return this
		 */
		DockerImageExporterBuilder path(String path);

		/**
		 * Sets the Docker context into which the built image should be imported. If omitted, the image is
		 * imported into the same context in which the build was executed.
		 *
		 * @param context the name of the context
		 * @return this
		 * @throws NullPointerException     if {@code context} is null
		 * @throws IllegalArgumentException if {@code context}'s format is invalid
		 */
		DockerImageExporterBuilder context(String context);
	}

	/**
	 * Builds an exporter that outputs images to a registry.
	 */
	interface RegistryExporterBuilder extends ImageExporterBuilder
	{
		@Override
		RegistryExporterBuilder name(String name);

		@Override
		RegistryExporterBuilder compressionType(CompressionType type);

		@Override
		RegistryExporterBuilder compressionLevel(int compressionLevel);
	}

	/**
	 * Builds an exporter that outputs images to disk using the OCI container format.
	 */
	interface OciImageExporterBuilder extends ImageExporterBuilder
	{
		@Override
		OciImageExporterBuilder name(String name);

		@Override
		OciImageExporterBuilder compressionType(CompressionType type);

		@Override
		OciImageExporterBuilder compressionLevel(int compressionLevel);

		/**
		 * Specifies that the image files should be written to a directory. By default, the image is packaged as a
		 * TAR archive, with {@code path} representing the archive’s location. When this method is used,
		 * {@code path} is treated as a directory, and image files are written directly into it.
		 *
		 * @return this
		 */
		OciImageExporterBuilder directory();

		/**
		 * Sets the Docker context into which the built image should be imported. If omitted, the image is
		 * imported into the same context in which the build was executed.
		 *
		 * @param context the name of the context
		 * @return this
		 * @throws NullPointerException     if {@code context} is null
		 * @throws IllegalArgumentException if {@code context}'s format is invalid
		 */
		OciImageExporterBuilder context(String context);
	}

	/**
	 * Represents the type of compression to apply to the output.
	 */
	enum CompressionType
	{
		/**
		 * Do not compress the output.
		 */
		UNCOMPRESSED,
		/**
		 * Compress the output using <a href="https://en.wikipedia.org/wiki/Gzip">gzip</a>.
		 */
		GZIP,
		/**
		 * Compress the output using
		 * <a href="https://github.com/containerd/stargz-snapshotter/blob/main/docs/estargz.md">eStargz</a>.
		 * <p>
		 * The {@code eStargz} format transforms a gzip-compressed layer into an equivalent tarball where each
		 * file is compressed individually. The system can retrieve each file without having to fetch and
		 * decompress the entire tarball.
		 */
		ESTARGZ,
		/**
		 * Compress the output using <a href="https://en.wikipedia.org/wiki/Zstd">zstd</a>.
		 */
		ZSTD;

		/**
		 * Returns the command-line representation of this option.
		 *
		 * @return the command-line value
		 */
		public String toCommandLine()
		{
			return name().toLowerCase(Locale.ROOT);
		}
	}
}