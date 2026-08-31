/**
 * Narrow shims that reproduce the observable behaviour dotenv used to get from Node's
 * standard library.
 *
 * <p>These are deliberately not general-purpose ports of {@code fs}, {@code os},
 * {@code path} and {@code process}. Each one reproduces only what dotenv actually
 * depends on, including the parts that are visible to callers — the exact wording of
 * {@code ENOENT} messages, the encodings {@code fs.readFileSync} accepts, and the
 * relative paths that end up in the {@code injected env} log line.
 */
package io.github.motdotla.dotenv.node;
