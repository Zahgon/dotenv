package io.github.motdotla.dotenv;

/** Options for {@link Dotenv#parse(String, ParseOptions)}. */
public final class ParseOptions {

  private boolean fast;

  /**
   * Use the faster character-scanner parser.
   *
   * <p>Default: {@code false}. Example:
   * {@code Dotenv.parse(src, new ParseOptions().fast(true))}
   */
  public ParseOptions fast(boolean fast) {
    this.fast = fast;
    return this;
  }

  public boolean fast() {
    return fast;
  }
}
