package io.github.cowwoc.anchor4j.core.internal.util;

import io.github.cowwoc.anchor4j.core.internal.client.Processes;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Path helper functions.
 */
public final class Paths
{
	/**
	 * Deletes a path recursively.
	 *
	 * @param path a path
	 * @throws IOException if an I/O error occurs
	 */
	public static void deleteRecursively(Path path) throws IOException
	{
		if (Files.notExists(path))
			return;
		Files.walkFileTree(path, new FileVisitor<>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
			{
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
				Files.deleteIfExists(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException
			{
				throw e;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException
			{
				if (e != null)
					throw e;
				Files.deleteIfExists(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Searches for executable files on the {@code PATH} environment variable.
	 * <p>
	 * A file is considered a match if it is executable and its filename matches one of the provided
	 * <p>
	 * Preference is given to filenames earlier in the list, even if a later one appears earlier in the
	 * {@code PATH}.
	 *
	 * @param filenames the set of filenames to accept
	 * @return {@code null} if no match was found
	 * @throws NullPointerException if {@code filenames} is null
	 */
	public static Path searchPath(List<String> filenames)
	{
		Path matchPath = null;
		int matchIndex = filenames.size();
		String suffix = getExecutableSuffix();
		for (String directory : System.getenv("PATH").split(File.pathSeparator))
		{
			// Strip leading/trailing quotes
			directory = directory.strip();
			if (directory.length() >= 2 &&
				(directory.startsWith("\"") && directory.endsWith("\"")) ||
				(directory.startsWith("'") && directory.endsWith("'")))
			{
				directory = directory.substring(1, directory.length() - 1);
			}

			for (int i = 0; i < matchIndex; ++i)
			{
				String filename = filenames.get(i);
				Path candidate = Path.of(directory).resolve(filename + suffix);
				if (Files.isRegularFile(candidate) && Files.isExecutable(candidate))
				{
					matchPath = candidate;
					matchIndex = i;
				}
			}
		}
		return matchPath;
	}

	/**
	 * Returns the suffix to append to the cmake executables.
	 *
	 * @return the suffix
	 */
	private static String getExecutableSuffix()
	{
		if (Processes.isWindows())
			return ".exe";
		return "";
	}

	private Paths()
	{
	}
}