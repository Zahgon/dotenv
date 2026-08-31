package io.github.motdotla.dotenv.node;

import java.util.Locale;

/** The slice of Node's {@code os} module that dotenv uses. */
public final class NodeOs {

  private NodeOs() {
  }

  /** The current user's home directory, equivalent to {@code os.homedir()}. */
  public static String homedir() {
    return System.getProperty("user.home");
  }

  /** The platform string Node would report: {@code darwin}, {@code linux} or {@code win32}. */
  public static String platform() {
    String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    if (name.startsWith("mac") || name.startsWith("darwin")) {
      return "darwin";
    }
    if (name.startsWith("windows")) {
      return "win32";
    }
    return "linux";
  }
}
