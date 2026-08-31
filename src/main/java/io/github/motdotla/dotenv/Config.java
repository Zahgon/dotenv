package io.github.motdotla.dotenv;

/**
 * Loads {@code .env} into {@link Dotenv#processEnv()} as a side effect of class loading —
 * the counterpart of {@code require('dotenv/config')}.
 *
 * <p>Touch it before your application code reads any configuration, either by calling
 * {@link #load()} or with {@code Class.forName("io.github.motdotla.dotenv.Config")}.
 * Configure it through the {@code DOTENV_CONFIG_*} environment variables.
 */
public final class Config {

  static {
    Dotenv.config();
  }

  private Config() {
  }

  /** Triggers the load. Calling it more than once has no further effect. */
  public static void load() {
    // The work happens in the static initializer, which runs once, on first touch.
  }
}
