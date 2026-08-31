package io.github.motdotla.dotenv;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A hand-written character scanner with no regular expressions in the hot path, opted into
 * with {@code fast}.
 *
 * <p>Via <a href="https://github.com/motdotla/dotenv/pull/1010">motdotla/dotenv#1010</a>.
 * It is held to producing exactly what {@link RegexParser} produces.
 */
final class FastParser {

  private FastParser() {
  }

  private static final char TAB = 0x09;
  private static final char LF = 0x0A;
  private static final char SPACE = 0x20;
  private static final char HASH = '#';
  private static final char SINGLE_QUOTE = '\'';
  private static final char DOUBLE_QUOTE = '"';
  private static final char BACKTICK = '`';
  private static final char BACKSLASH = '\\';
  private static final char EQUALS = '=';
  private static final char COLON = ':';
  private static final char BOM = 0xFEFF;

  private static final Pattern LINE_BREAKS = Pattern.compile("\\r\\n?");

  private static final boolean[] KEY_CHAR = new boolean[256];

  static {
    for (int i = '0'; i <= '9'; i++) {
      KEY_CHAR[i] = true;
    }
    for (int i = 'A'; i <= 'Z'; i++) {
      KEY_CHAR[i] = true;
    }
    for (int i = 'a'; i <= 'z'; i++) {
      KEY_CHAR[i] = true;
    }
    KEY_CHAR['-'] = true;
    KEY_CHAR['.'] = true;
    KEY_CHAR['_'] = true;
  }

  /** Characters outside Latin-1 are never key characters, matching the original lookup table. */
  private static boolean isKeyChar(int c) {
    return c >= 0 && c < KEY_CHAR.length && KEY_CHAR[c];
  }

  static Map<String, String> parse(String src) {
    Map<String, String> obj = new LinkedHashMap<>();

    String str = src.indexOf('\r') == -1 ? src : LINE_BREAKS.matcher(src).replaceAll("\n");
    final int len = str.length();
    int i = 0;

    while (i < len) {
      char c = str.charAt(i);

      // skip whitespace / blank lines (\r already normalized out)
      // the BOM (U+FEFF) is what editors on Windows write ahead of the first key — the
      // classic parser skips it as part of \s*, so do the same
      while (i < len && (c == SPACE || c == TAB || c == LF || c == BOM)) {
        i++;
        c = i < len ? str.charAt(i) : 0;
      }
      if (i >= len) {
        break;
      }

      // comment line
      if (c == HASH) {
        i = skipToEndOfLine(str, i, len);
        continue;
      }

      i = skipExportPrefix(str, i, len);

      // key: [A-Za-z0-9_.-]+ via lookup
      final int keyStart = i;
      int stop = 0;
      while (i < len) {
        stop = str.charAt(i);
        if (isKeyChar(stop)) {
          i++;
        } else {
          break;
        }
      }
      if (i == keyStart) {
        i = skipToEndOfLine(str, i, len);
        continue;
      }
      final String key = str.substring(keyStart, i);
      if (i >= len) {
        stop = 0;
      }

      // skip spaces/tabs before separator
      if (stop == SPACE || stop == TAB) {
        do {
          i++;
          stop = i < len ? str.charAt(i) : 0;
        } while (stop == SPACE || stop == TAB);
      }

      if (stop == EQUALS) {
        i++;
      } else if (stop == COLON && i + 1 < len
          && (str.charAt(i + 1) == SPACE || str.charAt(i + 1) == TAB)) {
        i++;
      } else {
        // invalid line — skip
        i = skipToEndOfLine(str, i, len);
        continue;
      }

      // skip spaces/tabs after separator
      while (i < len && (str.charAt(i) == SPACE || str.charAt(i) == TAB)) {
        i++;
      }

      c = i < len ? str.charAt(i) : 0;

      final Scan scan = c == SINGLE_QUOTE || c == DOUBLE_QUOTE || c == BACKTICK
          ? readQuoted(str, i, len, c)
          : readUnquoted(str, i, len);

      i = scan.next;
      obj.put(key, scan.value);
    }

    return obj;
  }

  /** A parsed value together with the index the scanner should carry on from. */
  private record Scan(String value, int next) {
  }

  private static int skipToEndOfLine(String str, int from, int len) {
    int i = from;
    while (i < len && str.charAt(i) != LF) {
      i++;
    }
    return i;
  }

  /** Consumes an optional {@code export} prefix followed by a space or tab. */
  private static int skipExportPrefix(String str, int from, int len) {
    if (str.charAt(from) != 'e' || from + 6 >= len || !str.startsWith("export", from)) {
      return from;
    }
    char next = str.charAt(from + 6);
    if (next != SPACE && next != TAB) {
      return from;
    }
    int i = from + 7;
    while (i < len && (str.charAt(i) == SPACE || str.charAt(i) == TAB)) {
      i++;
    }
    return i;
  }

  private static Scan readQuoted(String str, int from, int len, char quote) {
    final int valueStart = from + 1;
    int j = valueStart;
    while (j < len) {
      char cc = str.charAt(j);
      // a backslash consumes the next character when it escapes the quote (\") or another
      // backslash (\\). Without the \\ case the second backslash of a pair is left free to
      // escape a following closing quote, so a value ending in an escaped backslash —
      // VAR="C:\\dir\\" — runs past its own closing quote and swallows the rest of the file.
      if (cc == BACKSLASH && j + 1 < len) {
        char nc = str.charAt(j + 1);
        if (nc == quote || nc == BACKSLASH) {
          j += 2;
          continue;
        }
      }
      if (cc == quote) {
        break;
      }
      j++;
    }

    if (j >= len) {
      // unterminated quote — fall back to unquoted-from-here semantics
      return readUnquoted(str, from, len);
    }

    String value = str.substring(valueStart, j);
    if (quote == DOUBLE_QUOTE && value.indexOf(BACKSLASH) != -1) {
      value = value.replace("\\n", "\n").replace("\\r", "\r");
    }

    // trailing ws + optional comment
    int i = j + 1;
    while (i < len && (str.charAt(i) == SPACE || str.charAt(i) == TAB)) {
      i++;
    }
    if (i < len && str.charAt(i) == HASH) {
      i = skipToEndOfLine(str, i, len);
    }
    return new Scan(value, i);
  }

  private static Scan readUnquoted(String str, int from, int len) {
    int newline = str.indexOf(LF, from);
    if (newline == -1) {
      newline = len;
    }
    int hash = str.indexOf(HASH, from);
    if (hash == -1 || hash > newline) {
      hash = newline;
    }

    int end = hash;
    while (end > from) {
      char cc = str.charAt(end - 1);
      if (cc == SPACE || cc == TAB) {
        end--;
      } else {
        break;
      }
    }

    String value = from == end ? "" : str.substring(from, end);
    return new Scan(value, hash == newline ? hash : newline);
  }
}
