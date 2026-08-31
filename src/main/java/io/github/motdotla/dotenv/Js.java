package io.github.motdotla.dotenv;

import java.util.Locale;
import java.util.Set;

/**
 * The handful of JavaScript semantics the parser depends on and Java does not share.
 *
 * <p>The two that matter are whitespace and truthiness. JavaScript's {@code \s} and
 * {@code String.prototype.trim} cover a wider set of characters than Java's — notably the
 * BOM, which the classic parser relies on skipping ahead of the first key — and
 * JavaScript's boolean coercion is what decides whether a {@code DOTENV_CONFIG_*} value
 * turns an option on.
 */
final class Js {

  private Js() {
  }

  /**
   * A character class matching exactly what JavaScript's {@code \s} matches: the
   * {@code WhiteSpace} and {@code LineTerminator} productions. Java's own {@code \s} is
   * limited to {@code [ \t\n\x0B\f\r]} and would, among other things, leave a leading BOM
   * in front of the first key.
   */
  static final String WS =
      "[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]";

  /**
   * JavaScript's {@code LineTerminator} production: LF, CR, U+2028 and U+2029.
   *
   * <p>Java's regex engine uses a different set — it also counts U+0085 (NEL), and treats
   * {@code \r\n} as one terminator rather than two — so {@code ^}, {@code $} and {@code .}
   * under {@link java.util.regex.Pattern#MULTILINE} do not line up with JavaScript's. The
   * three constants below emulate JavaScript's anchors directly instead.
   */
  private static final String LINE_TERMINATORS = "\\r\\n\\u2028\\u2029";

  /** JavaScript's multiline {@code ^}: start of input, or just after a line terminator. */
  static final String CARET = "(?:\\A|(?<=[" + LINE_TERMINATORS + "]))";

  /** JavaScript's multiline {@code $}: end of input, or just before a line terminator. */
  static final String DOLLAR = "(?=[" + LINE_TERMINATORS + "]|\\z)";

  /** JavaScript's {@code .}: any character that is not a line terminator. */
  static final String DOT = "[^" + LINE_TERMINATORS + "]";

  private static final Set<String> FALSY_STRINGS = Set.of("false", "0", "no", "off", "");

  /** True when {@code c} is whitespace to JavaScript. */
  static boolean isWhitespace(char c) {
    if (c == 0x20 || (c >= 0x09 && c <= 0x0D)) {
      return true;
    }
    if (c < 0x80) {
      return false;
    }
    return c == 0x00A0
        || c == 0x1680
        || (c >= 0x2000 && c <= 0x200A)
        || c == 0x2028
        || c == 0x2029
        || c == 0x202F
        || c == 0x205F
        || c == 0x3000
        || c == 0xFEFF;
  }

  /** {@code String.prototype.trim}, which strips more than {@link String#strip()} does. */
  static String trim(String value) {
    int start = 0;
    int end = value.length();
    while (start < end && isWhitespace(value.charAt(start))) {
      start++;
    }
    while (end > start && isWhitespace(value.charAt(end - 1))) {
      end--;
    }
    return value.substring(start, end);
  }

  /**
   * Whether an option value turns its option on.
   *
   * <p>Strings go through dotenv's own list of falsy spellings — {@code false}, {@code 0},
   * {@code no}, {@code off} and empty — which is how {@code DOTENV_CONFIG_QUIET=off} reads
   * as off. Everything else follows JavaScript's {@code Boolean()}.
   */
  static boolean parseBoolean(Object value) {
    if (value instanceof String string) {
      return !FALSY_STRINGS.contains(string.toLowerCase(Locale.ROOT));
    }
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof Number number) {
      double d = number.doubleValue();
      return d != 0 && !Double.isNaN(d);
    }
    return true;
  }
}
