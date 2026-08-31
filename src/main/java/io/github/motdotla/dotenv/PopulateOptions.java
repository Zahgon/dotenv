package io.github.motdotla.dotenv;

/** Options for {@link Dotenv#populate(java.util.Map, java.util.Map, PopulateOptions)}. */
public final class PopulateOptions {

  private boolean debug;
  private boolean override;

  /**
   * Turn on logging to help debug why certain keys or values are not being set as you expect.
   *
   * <p>Default: {@code false}.
   */
  public PopulateOptions debug(boolean debug) {
    this.debug = debug;
    return this;
  }

  /**
   * Override any environment variables that have already been set with values from your
   * {@code .env} file.
   *
   * <p>Default: {@code false}.
   */
  public PopulateOptions override(boolean override) {
    this.override = override;
    return this;
  }

  public boolean debug() {
    return debug;
  }

  public boolean override() {
    return override;
  }
}
