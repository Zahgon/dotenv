package io.github.motdotla.dotenv.node;

/** The slice of Node's {@code process} global that dotenv uses. */
public final class NodeProcess {

  private NodeProcess() {
  }

  /**
   * The working directory, equivalent to {@code process.cwd()}.
   *
   * <p>The JVM cannot change its working directory after start-up, so unlike Node this
   * value is fixed for the life of the process.
   */
  public static String cwd() {
    return System.getProperty("user.dir");
  }
}
