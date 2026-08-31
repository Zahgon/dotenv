package io.github.motdotla.dotenv;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The classic, regular-expression based parser — dotenv's default.
 *
 * <p>The pattern is the original one with two changes forced by Java's regex engine.
 *
 * <p>Every {@code \s} is widened to {@link Js#WS}, so it keeps matching the characters
 * JavaScript calls whitespace — Java's own {@code \s} is narrower and would, among other
 * things, leave a leading BOM in front of the first key.
 *
 * <p>The {@code ^}, {@code $} and {@code .} anchors are spelled out via {@link Js#CARET},
 * {@link Js#DOLLAR} and {@link Js#DOT} rather than using {@code Pattern.MULTILINE}, because
 * Java counts U+0085 as a line terminator and JavaScript does not — enough to make a key
 * following a stray NEL parse here but not in the original.
 *
 * <p>The quoted-value alternatives are unrolled: {@code '(?:\\'|[^'])*'} is written as
 * {@code '[^']*(?:\\'[^']*)*'}. The two match the same language, but Java compiles an
 * alternation inside a greedy loop into a recursive matcher that costs one stack frame per
 * character, so the original spelling threw {@link StackOverflowError} on quoted values of a
 * few thousand characters — a private key or certificate, exactly what multiline values are
 * for. Unrolled, the bulk of the value is consumed by a flat character-class loop. Do not
 * fold it back.
 */
final class RegexParser {

  private RegexParser() {
  }

  private static final Pattern LINE = Pattern.compile(
      Js.CARET + Js.WS + "*(?:export" + Js.WS + "+)?([\\w.-]+)"
          + "(?:" + Js.WS + "*=" + Js.WS + "*?|:" + Js.WS + "+?)"
          + "(" + Js.WS + "*'[^']*(?:\\\\'[^']*)*'"
          + "|" + Js.WS + "*\"[^\"]*(?:\\\\\"[^\"]*)*\""
          + "|" + Js.WS + "*`[^`]*(?:\\\\`[^`]*)*`"
          + "|[^#\\r\\n]+)?"
          + Js.WS + "*(?:#" + Js.DOT + "*)?" + Js.DOLLAR);

  private static final Pattern SURROUNDING_QUOTES =
      Pattern.compile(Js.CARET + "(['\"`])([\\s\\S]*)\\1" + Js.DOLLAR);

  private static final Pattern LINE_BREAKS = Pattern.compile("\\r\\n?");

  static Map<String, String> parse(String src) {
    Map<String, String> obj = new LinkedHashMap<>();

    // Convert line breaks to same format
    String lines = src.indexOf('\r') == -1 ? src : LINE_BREAKS.matcher(src).replaceAll("\n");

    Matcher match = LINE.matcher(lines);
    while (match.find()) {
      String key = match.group(1);

      // Default undefined or null to empty string
      String value = match.group(2) == null ? "" : match.group(2);

      // Remove whitespace
      value = Js.trim(value);

      // Check if double quoted
      char maybeQuote = value.isEmpty() ? 0 : value.charAt(0);

      // Remove surrounding quotes. The pattern cannot match without a quote character
      // to open and close on, so a value with none is left alone rather than scanned.
      if (hasQuote(value)) {
        value = SURROUNDING_QUOTES.matcher(value).replaceAll("$2");
      }

      // Expand newlines if double quoted
      if (maybeQuote == '"') {
        value = value.replace("\\n", "\n");
        value = value.replace("\\r", "\r");
      }

      // Add to object
      obj.put(key, value);
    }

    return obj;
  }

  private static boolean hasQuote(String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '\'' || c == '"' || c == '`') {
        return true;
      }
    }
    return false;
  }
}
