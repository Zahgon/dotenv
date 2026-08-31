package io.github.motdotla.dotenv;

import java.util.Map;

/**
 * What {@link Dotenv#config()} returns: the keys it parsed, or the error that stopped it.
 *
 * <p>Both are nullable, mirroring the {@code { parsed?, error? }} object the JavaScript
 * implementation returned. A run that failed to read one of several files reports both.
 */
public final class ConfigResult {

  private final Map<String, String> parsed;
  private final RuntimeException error;

  ConfigResult(Map<String, String> parsed, RuntimeException error) {
    this.parsed = parsed;
    this.error = error;
  }

  /** The parsed keys and values, or {@code null} if nothing was parsed. */
  public Map<String, String> parsed() {
    return parsed;
  }

  /**
   * The last error encountered, or {@code null} if there was none.
   *
   * <p>Errors dotenv raises itself are {@link DotenvException}s carrying a
   * {@link DotenvException#code() code}.
   */
  public RuntimeException error() {
    return error;
  }
}
