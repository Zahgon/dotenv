package io.github.motdotla.dotenv;

/**
 * An error raised by dotenv, carrying the string {@code code} that identifies it.
 *
 * <p>Mirrors the {@code Error & { code }} objects the JavaScript implementation threw
 * and returned on {@link ConfigResult#error()}: {@code OBJECT_REQUIRED},
 * {@code SECURE_REQUIRES_DOTENVX}, and the {@code ENOENT}-style codes raised while
 * reading a {@code .env} file.
 */
public class DotenvException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String code;

  public DotenvException(String message, String code) {
    super(message);
    this.code = code;
  }

  /** The error code, for example {@code "ENOENT"}, or {@code null} when there is none. */
  public String code() {
    return code;
  }
}
