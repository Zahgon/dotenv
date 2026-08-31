package io.github.motdotla.dotenv.node;

import java.nio.file.Path;
import java.nio.file.Paths;

/** The slice of Node's {@code path} module that dotenv uses. */
public final class NodePath {

  private NodePath() {
  }

  /**
   * Resolves {@code parts} into an absolute path, equivalent to {@code path.resolve()}.
   */
  public static String resolve(String... parts) {
    Path resolved = Paths.get(NodeProcess.cwd());
    for (String part : parts) {
      if (part != null && !part.isEmpty()) {
        resolved = resolved.resolve(part);
      }
    }
    return resolved.normalize().toString();
  }

  /**
   * The relative path from {@code from} to {@code to}, equivalent to {@code path.relative()}.
   *
   * <p>Both arguments are validated the way Node validates them, so passing anything other
   * than a string — a {@code file:} URI used as a {@code path} option, say — throws rather
   * than silently stringifying.
   *
   * @throws IllegalArgumentException if either argument is not a {@code String}
   */
  public static String relative(Object from, Object to) {
    String fromPath = validateString(from, "from");
    String toPath = validateString(to, "to");
    Path base = Paths.get(fromPath).toAbsolutePath().normalize();
    Path target = Paths.get(toPath).toAbsolutePath().normalize();
    return base.relativize(target).toString();
  }

  /** Joins {@code parts} with the platform separator, equivalent to {@code path.join()}. */
  public static String join(String... parts) {
    Path joined = null;
    for (String part : parts) {
      if (part == null || part.isEmpty()) {
        continue;
      }
      joined = joined == null ? Paths.get(part) : Paths.get(joined.toString(), part);
    }
    return joined == null ? "." : joined.normalize().toString();
  }

  private static String validateString(Object value, String name) {
    if (value instanceof String string) {
      return string;
    }
    String received = value == null ? "null" : "an instance of " + value.getClass().getSimpleName();
    throw new IllegalArgumentException(
        "The \"" + name + "\" argument must be of type string. Received " + received);
  }
}
