package io.github.cowwoc.anchor4j.core.resource;

import io.github.cowwoc.anchor4j.core.exception.ContextNotFoundException;
import io.github.cowwoc.anchor4j.core.exception.UnsupportedExporterException;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.StringJoiner;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Represents an operation that builds an image.
 */
public interface CoreImageBuilder
{
	/**
	 * Sets the path of the {@code Dockerfile}. By default, the builder looks for the file in the current
	 * working directory.
	 *
	 * @param dockerfile the path of the {@code Dockerfile}
	 * @return this
	 * @throws NullPointerException if {@code dockerFile} is null
	 */
	CoreImageBuilder dockerfile(Path dockerfile);

	/**
	 * Adds a platform to build the image for.
	 *
	 * @param platform the platform of the image
	 * @return this
	 * @throws NullPointerException     if {@code platform} is null
	 * @throws IllegalArgumentException if {@code platform} contains whitespace or is empty
	 */
	CoreImageBuilder platform(String platform);

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
	CoreImageBuilder reference(String reference);

	/**
	 * Adds an external cache source to use. By default, no external cache sources are used.
	 *
	 * @param source the external cache source
	 * @return this
	 * @throws IllegalArgumentException if {@code source} contains whitespace, or is empty
	 * @see <a href="https://docs.docker.com/reference/cli/docker/buildx/build/#cache-from">Possible values</a>
	 */
	CoreImageBuilder cacheFrom(String source);

	/**
	 * Adds an output format and location for the image. By default, a build has no exporters, meaning the
	 * resulting image is discarded after the build completes. However, multiple exporters can be configured to
	 * export the image to one or more destinations.
	 *
	 * @param exporter the exporter
	 * @return this
	 * @throws NullPointerException if {@code exporter} is null
	 */
	CoreImageBuilder export(Exporter exporter);

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
	CoreImageBuilder builder(String builder);

	/**
	 * Adds a build listener.
	 *
	 * @param listener the build listener
	 * @return this
	 * @throws NullPointerException if {@code listener} is null
	 */
	CoreImageBuilder listener(BuildListener listener);

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
	CoreImage build(String buildContext) throws IOException, InterruptedException;

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
	CoreImage build(Path buildContext) throws IOException, InterruptedException;

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
	sealed interface Exporter
	{
		/**
		 * Returns the command-line representation of this option.
		 *
		 * @return the command-line value
		 */
		String toCommandLine();

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
			return new ContentsExporterBuilder(path);
		}

		/**
		 * Outputs the resulting image in Docker container format.
		 *
		 * @return the exporter
		 */
		@CheckReturnValue
		static DockerImageExporterBuilder dockerImage()
		{
			return new DockerImageExporterBuilder();
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
			return new OciImageExporterBuilder(path);
		}

		/**
		 * Pushes the resulting image to a registry.
		 *
		 * @return the exporter
		 */
		@CheckReturnValue
		static RegistryExporterBuilder registry()
		{
			return new RegistryExporterBuilder();
		}

		/**
		 * Returns the type of the exporter.
		 *
		 * @return the type
		 */
		String getType();

		/**
		 * Indicates if the exporter outputs an image.
		 *
		 * @return {@code true} if it outputs an image
		 */
		boolean outputsImage();
	}

	/**
	 * Builds an exporter that outputs the contents of images to disk.
	 */
	final class ContentsExporterBuilder extends ImageExporterBuilder
	{
		private final String path;
		private boolean directory;

		/**
		 * Creates a ContentsExporterBuilder.
		 *
		 * @param path the output location, which is either a TAR archive or a directory depending on whether
		 *             {@link #directory()} is invoked
		 * @throws NullPointerException     if {@code path} is null
		 * @throws IllegalArgumentException if {@code path} contains whitespace or is empty
		 */
		public ContentsExporterBuilder(String path)
		{
			requireThat(path, "path").doesNotContainWhitespace().isNotEmpty();
			this.path = path;
		}

		/**
		 * Specifies that the image files should be written to a directory. By default, the image is packaged as a
		 * TAR archive, with {@code path} representing the archive’s location. When this method is used,
		 * {@code path} is treated as a directory, and image files are written directly into it.
		 *
		 * @return this
		 */
		public ContentsExporterBuilder directory()
		{
			this.directory = true;
			return this;
		}

		/**
		 * Builds the exporter.
		 *
		 * @return the exporter
		 */
		public Exporter build()
		{
			return new ExporterAdapter();
		}

		private final class ExporterAdapter implements Exporter
		{
			@Override
			public String getType()
			{
				if (directory)
					return "local";
				return "tar";
			}

			@Override
			public boolean outputsImage()
			{
				return false;
			}

			@Override
			public String toCommandLine()
			{
				if (directory)
					return "type=local,dest=" + path;
				return "type=tar,dest=" + path;
			}
		}
	}

	/**
	 * Builds an exporter that outputs images.
	 */
	abstract class ImageExporterBuilder
	{
		/**
		 * The name of the image.
		 */
		protected String name = "";
		/**
		 * The type of compression to use.
		 */
		protected CompressionType compressionType = CompressionType.GZIP;
		/**
		 * The compression level to use.
		 */
		protected int compressionLevel = -1;

		/**
		 * Sets the image reference of this output. By default, the output name is derived from the image's tag,
		 * if specified; otherwise, the output remains unnamed.
		 *
		 * @param name the image reference
		 * @return this
		 * @throws NullPointerException     if {@code name} is null
		 * @throws IllegalArgumentException if {@code name} contains whitespace or is empty
		 */
		public ImageExporterBuilder name(String name)
		{
			requireThat(name, "name").doesNotContainWhitespace().isNotEmpty();
			this.name = name;
			return this;
		}

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
		public ImageExporterBuilder compressionType(CompressionType type)
		{
			requireThat(type, "type").isNotNull();
			this.compressionType = type;
			return this;
		}

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
		public ImageExporterBuilder compressionLevel(int compressionLevel)
		{
			switch (compressionType)
			{
				case UNCOMPRESSED ->
				{
				}
				case GZIP, ESTARGZ -> requireThat(compressionLevel, "compressionLevel").isBetween(0, 9);
				case ZSTD -> requireThat(compressionLevel, "compressionLevel").isBetween(0, 22);
			}
			this.compressionLevel = compressionLevel;
			return this;
		}
	}

	/**
	 * Builds an exporter that outputs images using the Docker container format.
	 */
	final class DockerImageExporterBuilder extends ImageExporterBuilder
	{
		private String path;
		private String context = "";

		/**
		 * Creates a ExportDockerImageBuilder.
		 */
		public DockerImageExporterBuilder()
		{
		}

		@Override
		public DockerImageExporterBuilder name(String name)
		{
			super.name(name);
			return this;
		}

		@Override
		public DockerImageExporterBuilder compressionType(CompressionType type)
		{
			super.compressionType(type);
			return this;
		}

		@Override
		public DockerImageExporterBuilder compressionLevel(int compressionLevel)
		{
			super.compressionLevel(compressionLevel);
			return this;
		}

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
		public DockerImageExporterBuilder path(String path)
		{
			requireThat(path, "path").doesNotContainWhitespace().isNotEmpty();
			this.path = path;
			return this;
		}

		/**
		 * Sets the Docker context into which the built image should be imported. If omitted, the image is
		 * imported into the same context in which the build was executed.
		 *
		 * @param context the name of the context
		 * @return this
		 * @throws NullPointerException     if {@code context} is null
		 * @throws IllegalArgumentException if {@code context}'s format is invalid
		 */
		public DockerImageExporterBuilder context(String context)
		{
			requireThat(context, "context").doesNotContainWhitespace().isNotEmpty();
			this.context = context;
			return this;
		}

		/**
		 * Builds the exporter.
		 *
		 * @return the exporter
		 */
		public Exporter build()
		{
			return new ExporterAdapter();
		}

		private final class ExporterAdapter implements Exporter
		{
			@Override
			public String getType()
			{
				return "docker";
			}

			@Override
			public boolean outputsImage()
			{
				return true;
			}

			@Override
			public String toCommandLine()
			{
				StringJoiner joiner = new StringJoiner(",");
				joiner.add("type=" + getType());
				if (path != null)
					joiner.add("dest=" + path);
				if (!name.isEmpty())
					joiner.add("name=" + name);
				if (compressionType != CompressionType.GZIP)
					joiner.add("compression=" + compressionType.toCommandLine());
				if (compressionLevel != -1)
					joiner.add("compression-level=" + compressionLevel);
				if (!context.isEmpty())
					joiner.add("context=" + context);
				return joiner.toString();
			}
		}
	}

	/**
	 * Builds an exporter that outputs images to a registry.
	 */
	final class RegistryExporterBuilder extends ImageExporterBuilder
	{
		/**
		 * Creates an RegistryExporterBuilder.
		 */
		public RegistryExporterBuilder()
		{
		}

		@Override
		public RegistryExporterBuilder name(String name)
		{
			super.name(name);
			return this;
		}

		@Override
		public RegistryExporterBuilder compressionType(
			CompressionType type)
		{
			super.compressionType(type);
			return this;
		}

		@Override
		public RegistryExporterBuilder compressionLevel(
			int compressionLevel)
		{
			super.compressionLevel(compressionLevel);
			return this;
		}

		/**
		 * Builds the output.
		 *
		 * @return the output
		 */
		public Exporter build()
		{
			return new ExporterAdapter();
		}

		private final class ExporterAdapter implements Exporter
		{
			@Override
			public String getType()
			{
				return "registry";
			}

			@Override
			public boolean outputsImage()
			{
				return true;
			}

			@Override
			public String toCommandLine()
			{
				StringJoiner joiner = new StringJoiner(",");
				joiner.add("type=registry");
				if (!name.isEmpty())
					joiner.add("name=" + name);
				if (compressionType != CompressionType.GZIP)
					joiner.add("compression=" + compressionType.toCommandLine());
				if (compressionLevel != -1)
					joiner.add("compression-level=" + compressionLevel);
				return joiner.toString();
			}
		}
	}

	/**
	 * Builds an exporter that outputs images to disk using the OCI container format.
	 */
	final class OciImageExporterBuilder extends ImageExporterBuilder
	{
		private final String path;
		private boolean directory;
		private String context = "";

		/**
		 * Creates a OciImageExporterBuilder.
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
		 *             {@link #directory() directory()} is invoked
		 */
		public OciImageExporterBuilder(String path)
		{
			requireThat(path, "path").doesNotContainWhitespace().isNotEmpty();
			this.path = path;
		}

		@Override
		public OciImageExporterBuilder name(String name)
		{
			super.name(name);
			return this;
		}

		@Override
		public OciImageExporterBuilder compressionType(
			CompressionType type)
		{
			super.compressionType(type);
			return this;
		}

		@Override
		public OciImageExporterBuilder compressionLevel(
			int compressionLevel)
		{
			super.compressionLevel(compressionLevel);
			return this;
		}

		/**
		 * Specifies that the image files should be written to a directory. By default, the image is packaged as a
		 * TAR archive, with {@code path} representing the archive’s location. When this method is used,
		 * {@code path} is treated as a directory, and image files are written directly into it.
		 *
		 * @return this
		 */
		public OciImageExporterBuilder directory()
		{
			this.directory = true;
			return this;
		}

		/**
		 * Sets the Docker context into which the built image should be imported. If omitted, the image is
		 * imported into the same context in which the build was executed.
		 *
		 * @param context the name of the context
		 * @return this
		 * @throws NullPointerException     if {@code context} is null
		 * @throws IllegalArgumentException if {@code context}'s format is invalid
		 */
		public OciImageExporterBuilder context(String context)
		{
			requireThat(context, "context").doesNotContainWhitespace().isNotEmpty();
			this.context = context;
			return this;
		}

		/**
		 * Builds the exporter.
		 *
		 * @return the exporter
		 */
		public Exporter build()
		{
			return new ExporterAdapter();
		}

		private final class ExporterAdapter implements Exporter
		{
			@Override
			public String getType()
			{
				return "oci";
			}

			@Override
			public boolean outputsImage()
			{
				return true;
			}

			@Override
			public String toCommandLine()
			{
				StringJoiner joiner = new StringJoiner(",");
				joiner.add("type=" + getType());
				if (path != null)
					joiner.add("dest=" + path);
				if (!name.isEmpty())
					joiner.add("name=" + name);
				if (directory)
					joiner.add("tar=false");
				if (compressionType != CompressionType.GZIP)
					joiner.add("compression=" + compressionType.toCommandLine());
				if (compressionLevel != -1)
					joiner.add("compression-level=" + compressionLevel);
				if (!context.isEmpty())
					joiner.add("context=" + context);
				return joiner.toString();
			}
		}
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