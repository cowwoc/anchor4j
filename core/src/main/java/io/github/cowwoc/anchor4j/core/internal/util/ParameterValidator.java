package io.github.cowwoc.anchor4j.core.internal.util;

import io.github.cowwoc.anchor4j.core.internal.client.ImageReferenceValidator;

import java.util.regex.Pattern;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * Validates common input parameters.
 */
public final class ParameterValidator
{
	// Based on https://github.com/moby/moby/blob/13879e7b496d14fb0724719c49c858731c9e7f60/daemon/names/names.go#L6
	private final static Pattern NAME_VALIDATOR = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_.-]+");
	// Based on https://github.com/distribution/reference/blob/727f80d42224f6696b8e1ad16b06aadf2c6b833b/regexp.go#L85
	final static Pattern ID_VALIDATOR = Pattern.compile("[a-f0-9]{64}");

	/**
	 * Validates a name.
	 *
	 * @param value the value of the name. The value must start with a letter, or digit, or underscore, and may
	 *              be followed by additional characters consisting of letters, digits, underscores, periods or
	 *              hyphens.
	 * @param name  the name of the value parameter
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code value}'s format is invalid
	 */
	public static void validateName(String value, String name)
	{
		assert name != null;
		requireThat(value, name).isNotNull();
		if (!NAME_VALIDATOR.matcher(value).matches())
			throw getNameException(value, name);
	}

	/**
	 * @param value the value of the name
	 * @param name  the name of the value parameter
	 * @return the exception to throw if a name's format is invalid
	 */
	private static IllegalArgumentException getNameException(String value, String name)
	{
		return new IllegalArgumentException(name + " must begin with a letter or number and may include " +
			"letters, numbers, underscores, periods, or hyphens. No other characters are allowed.\n" +
			"Value: " + value);
	}

	/**
	 * Validates an image reference.
	 *
	 * @param value the image's reference
	 * @param name  the name of the value parameter
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code value}'s format is invalid
	 */
	public static void validateImageReference(String value, String name)
	{
		assert name != null;
		requireThat(value, name).isNotNull();
		ImageReferenceValidator.validate(value, name);
	}

	/**
	 * Validates an image ID or reference.
	 *
	 * @param value the image's ID or reference
	 * @param name  the name of the value parameter
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code value}'s format is invalid
	 */
	public static void validateImageIdOrReference(String value, String name)
	{
		assert name != null;
		requireThat(value, name).isNotNull();
		if (ID_VALIDATOR.matcher(value).matches())
			return;
		ImageReferenceValidator.validate(value, name);
	}

	/**
	 * Validates a container ID or name.
	 * <p>
	 * Container names must start with a letter, or digit, or underscore, and may be followed by additional
	 * characters consisting of letters, digits, underscores, periods or hyphens. No other characters are
	 * allowed.
	 *
	 * @param value the container's ID or name
	 * @param name  the name of the value parameter
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code value}'s format is invalid
	 */
	public static void validateContainerIdOrName(String value, String name)
	{
		assert name != null;
		requireThat(value, name).isNotNull();
		if (ID_VALIDATOR.matcher(value).matches())
			return;
		validateName(value, name);
	}

	private ParameterValidator()
	{
	}
}