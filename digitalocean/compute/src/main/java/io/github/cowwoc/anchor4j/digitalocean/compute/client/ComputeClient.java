package io.github.cowwoc.anchor4j.digitalocean.compute.client;

import io.github.cowwoc.anchor4j.core.migration.DriftDetection;
import io.github.cowwoc.anchor4j.digitalocean.compute.internal.client.DefaultComputeClient;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.ComputeRegion;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.Droplet;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletCreator;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletImage;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.DropletType;
import io.github.cowwoc.anchor4j.digitalocean.compute.resource.SshPublicKey;
import io.github.cowwoc.anchor4j.digitalocean.core.client.DigitalOceanClient;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Region;
import io.github.cowwoc.anchor4j.digitalocean.network.resource.Vpc;
import io.github.cowwoc.requirements12.annotation.CheckReturnValue;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.List;
import java.util.function.Predicate;

/**
 * A DigitalOcean compute client.
 */
public interface ComputeClient extends DigitalOceanClient
{
	/**
	 * Returns a client.
	 *
	 * @param driftDetection the drift detection configuration
	 * @return the client
	 * @throws NullPointerException if {@code driftDetection} is null
	 * @throws IOException          if an I/O error occurs while building the client
	 */
	static ComputeClient build(DriftDetection driftDetection) throws IOException
	{
		return new DefaultComputeClient(driftDetection);
	}

	/**
	 * Returns the regions that match a predicate.
	 *
	 * @param predicate the predicate
	 * @return an empty list if no match is found
	 * @throws NullPointerException  if {@code predicate} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<ComputeRegion> getRegions(Predicate<ComputeRegion> predicate) throws IOException, InterruptedException;

	/**
	 * Returns the available regions.
	 *
	 * @param canCreateDroplets {@code true} if the returned types must be able to create droplets
	 * @return an empty list if no match is found
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<ComputeRegion> getRegions(boolean canCreateDroplets) throws IOException, InterruptedException;

	/**
	 * Looks up a region by its ID.
	 *
	 * @param id the ID of the region
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code id} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	ComputeRegion getRegion(Region.Id id) throws IOException, InterruptedException;

	/**
	 * Returns the first region that matches a predicate.
	 *
	 * @param predicate the predicate
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code predicate} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	ComputeRegion getRegion(Predicate<ComputeRegion> predicate) throws IOException, InterruptedException;

	/**
	 * Looks up the default VPC of a region.
	 *
	 * @param region the region
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code region} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Vpc getDefaultVpc(Region.Id region) throws IOException, InterruptedException;

	/**
	 * Returns all the droplet types that are available for creating droplets.
	 *
	 * @return an empty list if no match is found
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<DropletType> getDropletTypes() throws IOException, InterruptedException;

	/**
	 * Returns all the droplet types.
	 *
	 * @param canCreateDroplets {@code true} if the returned types must be able to create droplets
	 * @return an empty list if no match is found
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<DropletType> getDropletTypes(boolean canCreateDroplets) throws IOException, InterruptedException;

	/**
	 * Returns the first droplet type that matches a predicate.
	 *
	 * @param predicate the predicate
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code predicate} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	DropletType getDropletType(Predicate<DropletType> predicate)
		throws IOException, InterruptedException;

	/**
	 * Returns the first droplet image that matches a predicate.
	 *
	 * @param predicate the predicate
	 * @return null if no match is found
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted while waiting for a response. This can happen
	 *                              due to shutdown signals.
	 */
	DropletImage getDropletImage(Predicate<DropletImage> predicate) throws IOException, InterruptedException;

	/**
	 * Returns all the droplet images.
	 *
	 * @return null if no match is found
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted while waiting for a response. This can happen
	 *                              due to shutdown signals.
	 */
	List<DropletImage> getDropletImages() throws IOException, InterruptedException;

	/**
	 * Returns the droplet images that match a predicate.
	 *
	 * @param predicate the predicate
	 * @return an empty list if no match is found
	 * @throws IOException          if an I/O error occurs. These errors are typically transient, and retrying
	 *                              the request may resolve the issue.
	 * @throws InterruptedException if the thread is interrupted while waiting for a response. This can happen
	 *                              due to shutdown signals.
	 */
	List<DropletImage> getDropletImages(Predicate<DropletImage> predicate)
		throws IOException, InterruptedException;

	/**
	 * Looks up a droplet by its ID.
	 *
	 * @param id the ID
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code id} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Droplet getDroplet(Droplet.Id id) throws IOException, InterruptedException;

	/**
	 * Returns the first droplet that matches a predicate.
	 *
	 * @param predicate the predicate
	 * @return null if no match was found
	 * @throws NullPointerException     if {@code predicate} is null
	 * @throws IllegalArgumentException if any of the tags contain leading or trailing whitespace or are empty
	 * @throws IllegalStateException    if the client is closed
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	Droplet getDroplet(Predicate<Droplet> predicate) throws IOException, InterruptedException;

	/**
	 * Returns the all the droplets.
	 *
	 * @return an empty list if no match is found
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<Droplet> getDroplets() throws IOException, InterruptedException;

	/**
	 * Returns the droplets that match a predicate.
	 *
	 * @param predicate the predicate
	 * @return an empty list if no match is found
	 * @throws NullPointerException     if {@code predicate} is null
	 * @throws IllegalArgumentException if any of the tags contain leading or trailing whitespace or are empty
	 * @throws IllegalStateException    if the client is closed
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	List<Droplet> getDroplets(Predicate<Droplet> predicate) throws IOException, InterruptedException;

	/**
	 * Creates a droplet.
	 *
	 * @param name  the name of the droplet
	 * @param type  the machine type of the droplet
	 * @param image the image ID of a public or private image or the slug identifier for a public image that
	 *              will be used to boot this droplet
	 * @return a new droplet creator
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code anchorId} contains any characters other than letters,
	 *                                    numbers, colons, dashes and underscores.</li>
	 *                                    <li>is longer than 255 characters.</li>
	 *                                    <li>the {@code name} contains any characters other than {@code A-Z},
	 *                                    {@code a-z}, {@code 0-9} and a hyphen.</li>
	 *                                    <li>the {@code name} does not start or end with an alphanumeric
	 *                                    character.</li>
	 *                                    <li>any of the arguments contain leading or trailing whitespace or
	 *                                    are empty.</li>
	 *                                  </ul>
	 * @throws IllegalStateException    if the client is closed
	 * @see ComputeClient#getDefaultVpc(Region.Id)
	 */
	@CheckReturnValue
	DropletCreator createDroplet(String name, DropletType.Id type, DropletImage.Id image);

	/**
	 * Returns all the SSH keys.
	 *
	 * @return an empty list if no match is found
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<SshPublicKey> getSshPublicKeys() throws IOException, InterruptedException;

	/**
	 * Returns the SSH keys that match a predicate.
	 *
	 * @param predicate the predicate
	 * @return an empty list if no match is found
	 * @throws NullPointerException  if {@code predicate} null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	List<SshPublicKey> getSshPublicKeys(Predicate<SshPublicKey> predicate)
		throws IOException, InterruptedException;

	/**
	 * Returns the first SSH key that matches a predicate.
	 *
	 * @param predicate the predicate
	 * @return null if no match is found
	 * @throws NullPointerException  if {@code predicate} null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	SshPublicKey getSshPublicKey(Predicate<SshPublicKey> predicate) throws IOException, InterruptedException;

	/**
	 * Looks up an SSH key by its ID.
	 *
	 * @param id the ID of the public key
	 * @return null if no match was found
	 * @throws NullPointerException  if {@code id} is null
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	SshPublicKey getSshPublicKey(SshPublicKey.Id id) throws IOException, InterruptedException;

	/**
	 * Looks up an SSH key by its fingerprint.
	 *
	 * @param fingerprint the fingerprint of the public key
	 * @return null if no match was found
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>{@code fingerprint} contains leading or trailing whitespace or is
	 *                                    empty.</li>
	 *                                  </ul>
	 * @throws NullPointerException     if {@code fingerprint} is null
	 * @throws IllegalArgumentException if {@code fingerprint} contains leading or trailing whitespace or is
	 *                                  empty
	 * @throws IllegalStateException    if the client is closed
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	SshPublicKey getSshPublicKeyByFingerprint(String fingerprint) throws IOException, InterruptedException;

	/**
	 * Creates a new SSH key.
	 *
	 * @param name  the name of the public key
	 * @param value the value of the public key
	 * @return a new public key
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if:
	 *                                  <ul>
	 *                                    <li>any of the arguments contain leading or trailing whitespace or
	 *                                    are empty.</li>
	 *                                    <li>another SSH key with the same fingerprint already exists.</li>
	 *                                  </ul>
	 * @throws NullPointerException     if any of the arguments are null
	 * @throws IllegalArgumentException if {@code name} contains leading or trailing whitespace or is empty
	 * @throws IllegalStateException    if the client is closed
	 * @throws GeneralSecurityException if the key is unsupported or invalid
	 * @throws IOException              if an I/O error occurs. These errors are typically transient, and
	 *                                  retrying the request may resolve the issue.
	 * @throws InterruptedException     if the thread is interrupted while waiting for a response. This can
	 *                                  happen due to shutdown signals.
	 */
	@CheckReturnValue
	SshPublicKey createSshPublicKey(String name, PublicKey value)
		throws GeneralSecurityException, IOException, InterruptedException;

	/**
	 * Returns the ID of the droplet that the JVM is running on.
	 *
	 * @return null when running outside a droplet
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	Integer getDropletId() throws IOException, InterruptedException;

	/**
	 * Returns the hostname of the droplet that the JVM is running on.
	 *
	 * @return null when running outside a droplet
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	String getHostname() throws IOException, InterruptedException;

	/**
	 * Returns the region of the droplet that the JVM is running on.
	 *
	 * @return null when running outside a droplet
	 * @throws IllegalStateException if the client is closed
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               the request may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted while waiting for a response. This can happen
	 *                               due to shutdown signals.
	 */
	String getRegion() throws IOException, InterruptedException;
}