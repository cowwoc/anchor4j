package io.github.cowwoc.anchor4j.core.internal.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * String helper functions.
 */
public final class Strings
{
	public static final DateTimeFormatter HOUR_MINUTE_SECOND = DateTimeFormatter.ofPattern("H:mm:ss");
	public static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("H:mm");
	/**
	 * The regex pattern of a <a href="https://stackoverflow.com/a/6640851/14731">UUID</a>.
	 */
	public static final Pattern UUID = Pattern.compile(
		"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
	private static final ThreadLocal<DecimalFormat> FORMATTER = ThreadLocal.withInitial(() ->
	{
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.ENGLISH);
		DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();

		symbols.setGroupingSeparator('_');
		formatter.setDecimalFormatSymbols(symbols);
		return formatter;
	});

	/**
	 * @param value a number
	 * @return a string representation of the number with a visual separator every 3 digits
	 */
	public static String format(long value)
	{
		return FORMATTER.get().format(value);
	}

	/**
	 * Parses an {@code InetSocketAddress}.
	 *
	 * @param address the textual representation of the address
	 * @return the InetSocketAddress
	 * @throws NullPointerException     if {@code address} is null
	 * @throws IllegalArgumentException if {@code address}:
	 *                                  <ul>
	 *                                    <li>contains whitespace, or is empty.</li>
	 *                                    <li>is missing a port number.</li>
	 *                                  </ul>
	 */
	public static InetSocketAddress toInetSocketAddress(String address)
	{
		requireThat(address, "address").doesNotContainWhitespace().isNotEmpty();

		// https://stackoverflow.com/a/2347356/14731
		URI uri = URI.create("tcp://" + address);
		if (uri.getPort() == -1 ||
			(uri.getHost() == null && InetAddress.ofLiteral(uri.getAuthority()) instanceof Inet6Address))
		{
			throw new IllegalArgumentException("Address must contain a port number.\n" +
				"Actual: " + address);
		}
		return InetSocketAddress.createUnresolved(uri.getHost(), uri.getPort());
	}

	private Strings()
	{
	}
}