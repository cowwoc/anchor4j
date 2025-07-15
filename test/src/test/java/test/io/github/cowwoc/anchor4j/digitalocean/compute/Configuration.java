package test.io.github.cowwoc.anchor4j.digitalocean.compute;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * A configuration file.
 * <p>
 * <b>Thread-safety</b>: This class is thread-safe.
 */
public sealed class Configuration
{
	/**
	 * The configuration's key-value pairs.
	 */
	protected final Map<String, String> properties = new ConcurrentHashMap<>();
	private final String source;

	/**
	 * Returns an empty configuration.
	 *
	 * @return the configuration
	 */
	public static Configuration empty()
	{
		return new Configuration("empty");
	}

	/**
	 * Returns a configuration that reads values from a file.
	 *
	 * @param path the path of the configuration file
	 * @return the configuration
	 * @throws IOException if an I/O error occurs while loading the file
	 */
	public static Configuration fromPath(Path path) throws IOException
	{
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(path))
		{
			properties.load(in);
		}
		Configuration configuration = new Configuration("\"" + path + "\"");
		for (Entry<Object, Object> entry : properties.entrySet())
			configuration.put(entry.getKey().toString(), entry.getValue().toString());
		return configuration;
	}

	/**
	 * Returns a configuration that reads values from a file on the classpath.
	 * <p>
	 * This method is intended only for static, generated files (e.g., {@code git.properties}) that are bundled
	 * with the application. It should not be used for dynamic or arbitrary configuration, as individual files
	 * or directories cannot be selectively added to the module path - the module system scans all
	 * subdirectories and treats them as potential modules.
	 *
	 * @param classpath the path of the configuration file on the classpath
	 * @return the configuration
	 * @throws IOException if an I/O error occurs while loading the file
	 */
	public static Configuration fromClassPath(String classpath) throws IOException
	{
		ClassLoader classLoader = Configuration.class.getModule().getClassLoader();
		Properties properties = new Properties();
		try (InputStream resource = classLoader.getResourceAsStream(classpath))
		{
			if (resource == null)
			{
				throw new FileNotFoundException("Classpath entry not found: " + classpath + "\n" +
					"Classpath : " + System.getProperty("java.class.path") + "\n" +
					"Modulepath: " + System.getProperty("jdk.module.path"));
			}
			properties.load(resource);
		}
		Configuration configuration = new Configuration("classpath(\"" + classpath + "\")");
		for (Entry<Object, Object> entry : properties.entrySet())
			configuration.put(entry.getKey().toString(), entry.getValue().toString());
		return configuration;
	}

	/**
	 * Returns a configuration that reads values from environment variables.
	 *
	 * @return a new configuration
	 */
	public static Configuration fromEnvironmentVariables()
	{
		Configuration configuration = new Configuration("environment variables");
		for (Entry<String, String> entry : System.getenv().entrySet())
			configuration.put(entry.getKey(), entry.getValue());
		return configuration;
	}

	/**
	 * Merges multiple configurations, with the last value overriding any duplicate keys.
	 *
	 * @param configurations one or more configuration files
	 * @return a new configuration
	 */
	public static Configuration merge(Configuration... configurations)
	{
		requireThat(configurations, "configurations").isNotNull();

		StringJoiner joiner = new StringJoiner(", ");
		for (Configuration configuration : configurations)
			joiner.add(configuration.getSource());
		String source = "merge(" + joiner + ")";

		return new MergedConfiguration(source, configurations);
	}

	/**
	 * Creates a new configuration.
	 *
	 * @param source the source of the configuration properties
	 * @throws NullPointerException     if {@code source} is null
	 * @throws IllegalArgumentException if {@code source} contains leading or trailing whitespace or is empty
	 */
	private Configuration(String source)
	{
		requireThat(source, "source").isStripped().isNotEmpty();
		this.source = source;
	}

	/**
	 * Returns the source of the configuration properties.
	 *
	 * @return the source
	 */
	public String getSource()
	{
		return source;
	}

	/**
	 * Returns the keys contained within this configuration.
	 *
	 * @return the keys
	 */
	public Set<String> keySet()
	{
		return properties.keySet();
	}

	/**
	 * Associates the specified value with the specified key in this configuration. If the configuration
	 * previously contained a mapping for the key, the old value is replaced by the specified value.
	 *
	 * @param key   the key
	 * @param value the key
	 * @throws NullPointerException if any of the arguments are null
	 */
	public void put(String key, String value)
	{
		properties.put(key, value);
	}

	/**
	 * Returns the value of an optional configuration option.
	 *
	 * @param key          the name of the configuration option
	 * @param defaultValue the value to return if the configuration is absent
	 * @return the value of the configuration option
	 * @throws NullPointerException     if {@code key} is null
	 * @throws IllegalArgumentException if {@code key} contains leading, trailing whitespace or is empty
	 */
	public String getStringOrDefault(String key, String defaultValue)
	{
		requireThat(key, "key").isStripped().isNotEmpty();
		String value = properties.get(key);
		if (value == null)
			return defaultValue;
		return value;
	}

	/**
	 * Returns the value of a configuration option as a URI.
	 *
	 * @param key the name of the configuration option
	 * @return the value of the configuration option
	 * @throws NullPointerException     if {@code key} is null
	 * @throws IllegalArgumentException if {@code key} is blank. If the configuration did not contain the
	 *                                  specified key. If the value is not a valid URI.
	 */
	public URI getUri(String key)
	{
		String uriAsString = getString(key);
		return URI.create(uriAsString);
	}

	/**
	 * Returns the value of a configuration option consisting of a comma-separated list of values.
	 *
	 * @param key the name of the configuration option
	 * @return the value of the configuration option
	 * @throws NullPointerException     if {@code key} is null
	 * @throws IllegalArgumentException if {@code key} is blank. If the configuration did not contain the
	 *                                  specified key.
	 */
	public List<String> getList(String key)
	{
		String commaSeparatedList = getString(key);
		String[] individualValues = commaSeparatedList.split(",\\s*");
		return Arrays.asList(individualValues);
	}

	/**
	 * Returns the value of a configuration option.
	 *
	 * @param key the name of the configuration option
	 * @return the value of the configuration option
	 * @throws NullPointerException     if {@code key} is null
	 * @throws IllegalArgumentException if the configuration did not contain the specified key
	 */
	public String getString(String key)
	{
		String value = getStringOrDefault(key, null);
		if (value == null)
		{
			throw new IllegalArgumentException("configuration must contain \"" + key + "\".\n" +
				"Actual: null\n" +
				"Source: " + getSource() + "\n" +
				"Keys  : " + keySet());
		}
		return value;
	}

	/**
	 * Returns the value of a configuration option as an integer.
	 *
	 * @param key the name of the configuration option
	 * @return the value of the configuration option as an integer
	 * @throws NullPointerException     if {@code key} is null
	 * @throws NumberFormatException    if the value is not an integer
	 * @throws IllegalArgumentException if {@code key} contains leading, trailing whitespace or is blank. If the
	 *                                  configuration did not contain the specified key.
	 */
	public int getInt(String key)
	{
		return Integer.parseInt(getString(key));
	}

	/**
	 * Returns the value of an optional configuration option as an integer.
	 *
	 * @param key          the name of the configuration option
	 * @param defaultValue the value to return if the configuration is absent
	 * @return the value of the configuration option as an integer
	 * @throws NullPointerException     if {@code key} is null
	 * @throws NumberFormatException    if the value is not an integer
	 * @throws IllegalArgumentException if {@code key} contains leading, trailing whitespace or is blank
	 */
	public int getIntOrDefault(String key, int defaultValue)
	{
		String valueAsString = getStringOrDefault(key, null);
		if (valueAsString == null)
			return defaultValue;
		return Integer.parseInt(valueAsString);
	}

	/**
	 * Returns the value of a configuration option as a double.
	 *
	 * @param key the name of the configuration option
	 * @return the value of the configuration option as a double
	 * @throws NullPointerException     if {@code key} is null
	 * @throws NumberFormatException    if the value is not a double
	 * @throws IllegalArgumentException if {@code key} contains leading, trailing whitespace or is blank. If the
	 *                                  configuration did not contain the specified key.
	 */
	public double getDouble(String key)
	{
		return Double.parseDouble(getString(key));
	}

	/**
	 * Returns the value of an optional configuration option as a double.
	 *
	 * @param key          the name of the configuration option
	 * @param defaultValue the value to return if the configuration is absent
	 * @return the value of the configuration option as a double
	 * @throws NullPointerException     if {@code key} is null
	 * @throws NumberFormatException    if the value is not a double
	 * @throws IllegalArgumentException if {@code key} contains leading, trailing whitespace or is blank
	 */
	public double getDoubleOrDefault(String key, double defaultValue)
	{
		String valueAsString = getStringOrDefault(key, null);
		if (valueAsString == null)
			return defaultValue;
		return Double.parseDouble(valueAsString);
	}

	/**
	 * Returns a {@code Map} representation of the configuration.
	 *
	 * @return the {@code Map} representation
	 */
	public Map<String, String> asMap()
	{
		Map<String, String> map = new HashMap<>();
		for (String key : keySet())
		{
			String value = getString(key);
			map.put(key, value);
		}
		return map;
	}

	/**
	 * Returns a {@code String} representation of the configuration as a properties file.
	 *
	 * @return the {@code String} representation
	 */
	public String asProperties()
	{
		StringJoiner joiner = new StringJoiner(System.lineSeparator());
		for (Entry<String, String> entry : asMap().entrySet())
			joiner.add(entry.getKey() + "=" + entry.getValue());
		return joiner.toString();
	}

	@Override
	public String toString()
	{
		return asMap().toString();
	}

	/**
	 * Returns a subset of this configuration.
	 *
	 * @param keys the keys that the returned configuration may inherit
	 * @return the sub-configuration
	 */
	public Configuration filter(String... keys)
	{
		requireThat(keys, "keys").isNotNull();

		StringJoiner joiner = new StringJoiner(", ");
		for (String key : keys)
			joiner.add(key);
		String source = "filter(" + joiner + ")";

		return new FilteredConfiguration(source, this, new HashSet<>(Arrays.asList(keys)));
	}

	/**
	 * Merges multiple configurations, with the last value overriding any duplicate keys.
	 */
	public static final class MergedConfiguration extends Configuration
	{
		private final Configuration[] configurations;

		/**
		 * Creates a new instance.
		 *
		 * @param source         a human-readable description of the configurations that are being merged
		 * @param configurations the configurations
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code source} contains leading or trailing whitespace or is empty
		 */
		private MergedConfiguration(String source, Configuration... configurations)
		{
			super(source);
			requireThat(configurations, "configurations").isNotNull();
			this.configurations = configurations;
		}

		@Override
		public Set<String> keySet()
		{
			Set<String> keys = new HashSet<>(properties.keySet());
			for (Configuration configuration : configurations)
				keys.addAll(configuration.keySet());
			return keys;
		}

		@Override
		public String getStringOrDefault(String key, String defaultValue)
		{
			String value = defaultValue;
			for (Configuration configuration : configurations)
			{
				String newValue = configuration.getStringOrDefault(key, null);
				if (newValue != null)
					value = newValue;
			}
			value = properties.getOrDefault(key, value);
			return value;
		}

		@Override
		public String toString()
		{
			List<Configuration> concat = new ArrayList<>();
			Collections.addAll(concat, configurations);
			return concat.toString();
		}
	}

	/**
	 * Exposes a subset of a configuration.
	 */
	public static final class FilteredConfiguration extends Configuration
	{
		private final Configuration delegate;
		private final Set<String> filter;

		/**
		 * Creates a new instance.
		 *
		 * @param source   a human-readable description of the configurations that are being merged
		 * @param delegate the configuration to delegate to
		 * @param filter   the keys to inherit from {@code delegate}
		 * @throws NullPointerException     if any of the arguments are null
		 * @throws IllegalArgumentException if {@code source} contains leading or trailing whitespace or is empty
		 */
		private FilteredConfiguration(String source, Configuration delegate, Set<String> filter)
		{
			super(source);
			requireThat(delegate, "delegate").isNotNull();
			requireThat(filter, "filter").isNotNull();
			this.delegate = delegate;
			this.filter = Set.copyOf(filter);
		}

		@Override
		public Set<String> keySet()
		{
			Set<String> keys = new HashSet<>(delegate.keySet());
			keys.retainAll(filter);
			return keys;
		}

		@Override
		public String getStringOrDefault(String key, String defaultValue)
		{
			if (!filter.contains(key))
				return defaultValue;
			return delegate.getStringOrDefault(key, defaultValue);
		}
	}
}