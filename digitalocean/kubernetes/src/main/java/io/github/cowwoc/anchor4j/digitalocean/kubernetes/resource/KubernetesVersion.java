package io.github.cowwoc.anchor4j.digitalocean.kubernetes.resource;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;

/**
 * The version number of the Kubernetes software.
 */
public enum KubernetesVersion
{
	/**
	 * Version 1.29.9.
	 */
	V1_29_9,
	/**
	 * Version 1.30.5.
	 */
	V1_30_5,
	/**
	 * Version 1.31.1.
	 */
	V1_31_1;

	/**
	 * Looks up a value from its JSON representation.
	 *
	 * @param json the JSON representation
	 * @return the matching value
	 * @throws NullPointerException     if {@code json} is null
	 * @throws IllegalArgumentException if no match is found
	 */
	public static KubernetesVersion fromJson(JsonNode json)
	{
		// Sample value: 1.29.9-do.5
		String version = json.textValue();
		if (!version.endsWith("-do.5"))
		{
			// Unknown format
			return valueOf(version);
		}
		return valueOf("V" + version.substring(0, version.length() - "-do.5".length()).
			replace('.', '_').toUpperCase(Locale.ROOT));
	}

	/**
	 * Returns the version's JSON representation (slug).
	 *
	 * @return the JSON representation
	 */
	public String toJson()
	{
		return name().substring(1).replace('_', '.') + "-do.5";
	}

	@Override
	public String toString()
	{
		return toJson();
	}
}