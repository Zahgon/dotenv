package io.github.motdotla.dotenv;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A minimal reader for the flat JSON object dotenvx prints.
 *
 * <p>dotenv has never had a runtime dependency and this port keeps it that way, so rather
 * than pull in a JSON library for one integration point it reads exactly the shape dotenvx
 * emits: an object of environment keys to scalar values. Nested objects and arrays are not
 * environment values, so they are skipped rather than flattened, and {@code null} is
 * skipped the way an absent JavaScript value would be.
 */
final class Json {

  private final String src;
  private int pos;

  private Json(String src) {
    this.src = src;
  }

  static Map<String, String> parseObject(String text) {
    Json json = new Json(text);
    json.skipWhitespace();
    Map<String, String> result = json.readObject();
    json.skipWhitespace();
    if (json.pos != json.src.length()) {
      throw json.error("unexpected trailing content");
    }
    return result;
  }

  private Map<String, String> readObject() {
    expect('{');
    Map<String, String> result = new LinkedHashMap<>();
    skipWhitespace();
    if (peek() == '}') {
      pos++;
      return result;
    }
    while (true) {
      skipWhitespace();
      String key = readString();
      skipWhitespace();
      expect(':');
      skipWhitespace();
      String value = readValue();
      if (value != null) {
        result.put(key, value);
      }
      skipWhitespace();
      char c = read();
      if (c == '}') {
        return result;
      }
      if (c != ',') {
        throw error("expected ',' or '}'");
      }
    }
  }

  private String readValue() {
    switch (peek()) {
      case '"':
        return readString();
      case '{':
        skipContainer('{', '}');
        return null;
      case '[':
        skipContainer('[', ']');
        return null;
      case 't':
        expectLiteral("true");
        return "true";
      case 'f':
        expectLiteral("false");
        return "false";
      case 'n':
        expectLiteral("null");
        return null;
      default:
        return readNumber();
    }
  }

  private String readString() {
    expect('"');
    StringBuilder out = new StringBuilder();
    while (true) {
      char c = read();
      if (c == '"') {
        return out.toString();
      }
      if (c != '\\') {
        out.append(c);
        continue;
      }
      char escape = read();
      switch (escape) {
        case '"':
        case '\\':
        case '/':
          out.append(escape);
          break;
        case 'b':
          out.append('\b');
          break;
        case 'f':
          out.append('\f');
          break;
        case 'n':
          out.append('\n');
          break;
        case 'r':
          out.append('\r');
          break;
        case 't':
          out.append('\t');
          break;
        case 'u':
          out.append(readUnicodeEscape());
          break;
        default:
          throw error("invalid escape '\\" + escape + "'");
      }
    }
  }

  private char readUnicodeEscape() {
    if (pos + 4 > src.length()) {
      throw error("truncated unicode escape");
    }
    String hex = src.substring(pos, pos + 4);
    pos += 4;
    try {
      return (char) Integer.parseInt(hex, 16);
    } catch (NumberFormatException e) {
      throw error("invalid unicode escape '\\u" + hex + "'");
    }
  }

  private String readNumber() {
    int start = pos;
    while (pos < src.length() && "+-.eE0123456789".indexOf(src.charAt(pos)) != -1) {
      pos++;
    }
    if (start == pos) {
      throw error("unexpected character '" + peek() + "'");
    }
    return src.substring(start, pos);
  }

  private void skipContainer(char open, char close) {
    expect(open);
    int depth = 1;
    while (depth > 0) {
      char c = read();
      if (c == '"') {
        pos--;
        readString();
      } else if (c == open) {
        depth++;
      } else if (c == close) {
        depth--;
      }
    }
  }

  private void expectLiteral(String literal) {
    if (!src.startsWith(literal, pos)) {
      throw error("expected '" + literal + "'");
    }
    pos += literal.length();
  }

  private void expect(char expected) {
    char c = read();
    if (c != expected) {
      throw error("expected '" + expected + "' but found '" + c + "'");
    }
  }

  private char peek() {
    if (pos >= src.length()) {
      throw error("unexpected end of input");
    }
    return src.charAt(pos);
  }

  private char read() {
    char c = peek();
    pos++;
    return c;
  }

  private void skipWhitespace() {
    while (pos < src.length()) {
      char c = src.charAt(pos);
      if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
        pos++;
      } else {
        return;
      }
    }
  }

  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException("invalid JSON at offset " + pos + ": " + message);
  }
}
