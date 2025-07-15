package io.github.cowwoc.anchor4j.core.migration;

import io.github.cowwoc.anchor4j.core.exception.AccessDeniedException;

import java.io.IOException;

/**
 * Represents a cloud migration.
 * <p>
 * A migration provides mechanisms to apply changes to cloud infrastructure and validate that the resulting
 * state matches expectations.
 * <p>
 * Migration class names must follow the convention:
 * <pre>{@code
 * V<version>__<description>
 * }</pre>
 * where:
 * <ul>
 *   <li>{@code <version>} is a sequential version number composed of one or more numeric components
 *   separated by a single underscore (e.g., {@code 1}, {@code 1_2}, {@code 1_2_3}). Leading zeros in each
 *   component are ignored.</li>
 *   <li>{@code <description>} is a human-readable description of the migration's purpose separated
 *   using camel case or underscores.</li>
 * </ul>
 * For example: {@code V1__AddLoadBalancer}, {@code V1_2__UpdateFirewallRules}.
 * <p>
 * If different migrations use a different number of version components, unspecified components are
 * considered to be zero for comparison purposes. For example, version {@code 1_2} is treated as equivalent
 * to version {@code 1_2_0}.
 * <p>
 * <strong>Lifecycle Overview:</strong>
 * <ol>
 *   <li><strong>Migrate</strong>:
 *     <ul>
 *       <li>{@link #migrate()} is invoked to update the infrastructure to the desired state.</li>
 *     </ul>
 *   </li>
 *   <li><strong>Drift Detection</strong>:
 *     <ul>
 *       <li>After migration completes, {@link #driftDetection()} is invoked to verify that the desired state
 *           has been achieved and to detect any configuration drift or unintended changes.</li>
 *       <li>If drift detection passes, the migration is marked as successful.</li>
 *       <li>If drift detection fails, the migration is marked as failed.</li>
 *     </ul>
 *   </li>
 *   <li><strong>Re-execution on Failure</strong>:
 *     <ul>
 *       <li>On subsequent migration runs, any migration marked as failed is treated as not yet applied,
 *           and the entire sequence (migrate, drift detection) is executed again.</li>
 *     </ul>
 *   </li>
 * </ol>
 * <p>
 * <strong>Recommended Migration Strategy:</strong>
 * <p>
 * To minimize risk and ensure zero-downtime deployments, migrations are encouraged to follow a
 * stepwise, backward-compatible approach similar to the strategy outlined by PlanetScale:
 * <a href="https://planetscale.com/blog/backward-compatible-databases-changes">
 * https://planetscale.com/blog/backward-compatible-databases-changes
 * </a>.
 * <p>
 * Specifically, consider creating a separate migration for each of the following phases:
 * <ol>
 *   <li><strong>Expand the schema:</strong> Create any new cloud resources needed to support the upcoming
 *   changes.</li>
 *   <li><strong>Expand the application code:</strong> Deploy application changes that write to both the old
 *   and new resources in parallel.</li>
 *   <li><strong>Migrate the data:</strong> Copy or transform data from the old resources into the new ones,
 *   ensuring consistency.</li>
 *   <li><strong>Migrate the application code:</strong> Update application logic to read exclusively from the
 *   new resources.</li>
 *   <li><strong>Contract the application:</strong> Stop writing to the old resources.</li>
 *   <li><strong>Contract the schema:</strong> Safely remove the obsolete resources.</li>
 * </ol>
 * <p>
 * This staged approach helps avoid downtime, facilitates rollback, and ensures that each migration step
 * is clear, isolated, and idempotent.
 */
public interface Migration
{
	/**
	 * Updates the cloud infrastructure to reach the desired state.
	 * <p>
	 * Implementations must be <em>idempotent</em>: invoking this method multiple times must always result in
	 * the same final state. Specifically:
	 * <ul>
	 *   <li>The implementation must tolerate resources already existing without failing or creating
	 *   duplicates.</li>
	 *   <li>It must not overwrite or modify existing data unintentionally.</li>
	 *   <li>It must avoid any logic that produces non-deterministic results over time, such as selecting
	 *       resources based on dynamic criteria (e.g., choosing the cheapest machine type at the moment of
	 *       execution).</li>
	 * </ul>
	 * <p>
	 * To ensure determinism and reproducibility, consider dividing the process into two phases:
	 * <ol>
	 *   <li><strong>Discovery phase:</strong> Determine the exact configuration you intend to deploy
	 *   (for example, identify the cheapest available machine type).</li>
	 *   <li><strong>Execution phase:</strong> Create a migration script or configuration that pins these
	 *   choices explicitly so that running the migration later will yield identical results.</li>
	 * </ol>
	 *
	 * @param driftDetection the drift detection session
	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
	 *                               may resolve the issue.
	 * @throws InterruptedException  if the thread is interrupted before the operation completes.
	 * @throws AccessDeniedException if the client does not have sufficient privileges to execute this request
	 */
	void migrate(DriftDetection driftDetection) throws IOException, InterruptedException, AccessDeniedException;
//
//	/**
//	 * Detects whether the actual state of the cloud deployment matches the desired state and identifies any
//	 * configuration drift.
//	 * <p>
//	 * This method is invoked after {@link #migrate()} completes to verify that the migration was successful.
//	 * <p>
//	 * Implementations are strongly encouraged to inspect all resources across all regions, not just those
//	 * explicitly referenced by migration scripts. This comprehensive validation helps detect manual changes or
//	 * external processes that have altered the infrastructure.
//	 *
//	 * @throws IOException           if an I/O error occurs. These errors are typically transient, and retrying
//	 *                               may resolve the issue.
//	 * @throws InterruptedException  if the thread is interrupted before the operation completes.
//	 * @throws AccessDeniedException if the client does not have sufficient privileges to execute this request
//	 */
//	void driftDetection() throws IOException, InterruptedException, AccessDeniedException;
}