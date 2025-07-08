package io.github.cowwoc.anchor4j.digitalocean.compute.internal.util;

import org.apache.sshd.common.config.keys.PublicKeyEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.StringJoiner;

import static io.github.cowwoc.requirements12.java.DefaultJavaValidators.requireThat;

/**
 * SSH helper functions.
 */
@SuppressWarnings({"checkstyle:javadocblocktaglocation", "checkstyle:javadocmissingleadingasterisk",
	"checkstyle:javadocmissingwhitespaceafterasterisk", "checkstyle:javadoctagcontinuationindentation",
	"checkstyle:nonemptyatclausedescription", "checkstyle:requireemptylinebeforeblocktaggroup",
	"checkstyle:singlelinejavadoc"})
// WORKAROUND: https://github.com/checkstyle/checkstyle/issues/16005
public class SshKeys
{
	/**
	 * Creates a new instance.
	 */
	public SshKeys()
	{
	}

	/**
	 * Writes a {@code PublicKey} as a PEM-encoded OpenSSH format stream.
	 *
	 * @param publicKey the public key
	 * @param out       the stream to write into
	 * @param comment   the comment to append to the end of the key
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IOException              if an error occurs while writing into the stream
	 * @throws GeneralSecurityException if the key pair is unsupported or invalid
	 */
	public void writePublicKeyAsOpenSsh(PublicKey publicKey, String comment, OutputStream out)
		throws IOException, GeneralSecurityException
	{
		requireThat(comment, "comment").isStripped();
		try (OutputStreamWriter writer = new OutputStreamWriter(out))
		{
			writer.write(PublicKeyEntry.toString(publicKey));
			if (!comment.isEmpty())
				writer.write(" " + comment);
		}
		catch (IllegalArgumentException e)
		{
			throw new GeneralSecurityException(e);
		}
	}

	/**
	 * Writes a {@code PublicKey} fingerprint as a OpenSSH format stream.
	 *
	 * @param publicKey     a public key
	 * @param messageDigest the digest algorithm to use (e.g. MD5 or SHA256)
	 * @param out           the stream to write into
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IOException              if an error occurs while writing into the stream
	 * @throws GeneralSecurityException if the key pair is unsupported or invalid
	 */
	public void writeFingerprint(PublicKey publicKey, MessageDigest messageDigest, OutputStream out)
		throws IOException, GeneralSecurityException
	{
		String keyAsBase64 = getKeyAsBase64(publicKey);
		byte[] keyBytes = Base64.getDecoder().decode(keyAsBase64);
		byte[] fingerprintAsBytes = messageDigest.digest(keyBytes);

		try (Writer writer = new OutputStreamWriter(out))
		{
			writer.write(getKeySize(publicKey) + " ");

			String algorithm = messageDigest.getAlgorithm();
			String fingerprint;
			if (algorithm.equals("MD5"))
			{
				StringJoiner hexFingerprint = new StringJoiner(":");
				hexFingerprint.add(algorithm);
				for (byte b : fingerprintAsBytes)
					hexFingerprint.add(String.format("%02x", b));
				fingerprint = hexFingerprint.toString();
			}
			else
			{
				fingerprint = algorithm + ":" + Base64.getEncoder().withoutPadding().
					encodeToString(fingerprintAsBytes);
			}
			writer.write(fingerprint);
		}
	}

	/**
	 * Returns the base64-encoded OpenSSH representation of the key.
	 *
	 * @param publicKey the key
	 * @return the base64-encoded OpenSSH representation of the key.
	 * @throws GeneralSecurityException if the key pair is unsupported or invalid
	 */
	private String getKeyAsBase64(PublicKey publicKey) throws GeneralSecurityException
	{
		String publicKeyAsOpenSsh;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			writePublicKeyAsOpenSsh(publicKey, "", out);
			publicKeyAsOpenSsh = out.toString();
		}
		catch (IOException e)
		{
			// Exception never thrown by ByteArrayOutputStream
			throw new AssertionError(e);
		}
		// Extract the base64 part of the OpenSSH public key
		return publicKeyAsOpenSsh.split(" ")[1];
	}

	/**
	 * Returns the key size in bits.
	 *
	 * @param key a key
	 * @return the key size in bits
	 */
	private int getKeySize(PublicKey key)
	{
		return switch (key)
		{
			case RSAPublicKey rsa -> rsa.getModulus().bitLength();
			case ECPublicKey ec -> ec.getParams().getCurve().getField().getFieldSize();
			case DSAPublicKey dsa -> dsa.getParams().getP().bitLength();
			default -> throw new UnsupportedOperationException("Unsupported type: " + key.getClass().getName());
		};
	}
}