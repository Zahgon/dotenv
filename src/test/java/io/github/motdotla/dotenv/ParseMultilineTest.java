package io.github.motdotla.dotenv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.motdotla.dotenv.node.NodeFs;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Ported from {@code tests/test-parse-multiline.js}. */
class ParseMultilineTest {

  private static Map<String, String> parsed;

  @BeforeAll
  static void parseFixture() {
    parsed = Dotenv.parse(NodeFs.readFileSync("tests/.env.multiline", "utf8"));
  }

  @Test
  @DisplayName("should return an object")
  void returnsAnObject() {
    assertInstanceOf(Map.class, parsed, "should return an object");
  }

  @Test
  @DisplayName("sets basic environment variable")
  void setsBasicEnvironmentVariable() {
    assertEquals("basic", parsed.get("BASIC"), "sets basic environment variable");
  }

  @Test
  @DisplayName("reads after a skipped line")
  void readsAfterASkippedLine() {
    assertEquals("after_line", parsed.get("AFTER_LINE"), "reads after a skipped line");
  }

  @Test
  @DisplayName("defaults empty values to empty string")
  void defaultsEmptyToEmptyString() {
    assertEquals("", parsed.get("EMPTY"), "defaults empty values to empty string");
  }

  @Test
  @DisplayName("escapes single quoted values")
  void escapesSingleQuotedValues() {
    assertEquals("single_quotes", parsed.get("SINGLE_QUOTES"), "escapes single quoted values");
  }

  @Test
  @DisplayName("respects surrounding spaces in single quotes")
  void respectsSurroundingSpacesInSingleQuotes() {
    assertEquals("    single quotes    ", parsed.get("SINGLE_QUOTES_SPACED"),
        "respects surrounding spaces in single quotes");
  }

  @Test
  @DisplayName("escapes double quoted values")
  void escapesDoubleQuotedValues() {
    assertEquals("double_quotes", parsed.get("DOUBLE_QUOTES"), "escapes double quoted values");
  }

  @Test
  @DisplayName("respects surrounding spaces in double quotes")
  void respectsSurroundingSpacesInDoubleQuotes() {
    assertEquals("    double quotes    ", parsed.get("DOUBLE_QUOTES_SPACED"),
        "respects surrounding spaces in double quotes");
  }

  @Test
  @DisplayName("expands newlines but only if double quoted")
  void expandsNewlinesWhenDoubleQuoted() {
    assertEquals("expand\nnew\nlines", parsed.get("EXPAND_NEWLINES"),
        "expands newlines but only if double quoted");
  }

  @Test
  @DisplayName("expands newlines but only if double quoted (unquoted)")
  void doesNotExpandNewlinesWhenUnquoted() {
    assertEquals("dontexpand\\nnewlines", parsed.get("DONT_EXPAND_UNQUOTED"),
        "expands newlines but only if double quoted");
  }

  @Test
  @DisplayName("expands newlines but only if double quoted (single quoted)")
  void doesNotExpandNewlinesWhenSingleQuoted() {
    assertEquals("dontexpand\\nnewlines", parsed.get("DONT_EXPAND_SQUOTED"),
        "expands newlines but only if double quoted");
  }

  @Test
  @DisplayName("ignores commented lines")
  void ignoresCommentedLines() {
    assertNull(parsed.get("COMMENTS"), "ignores commented lines");
  }

  @Test
  @DisplayName("respects equals signs in values")
  void respectsEqualsSignsInValues() {
    assertEquals("equals==", parsed.get("EQUAL_SIGNS"), "respects equals signs in values");
  }

  @Test
  @DisplayName("retains inner quotes")
  void retainsInnerQuotes() {
    assertEquals("{\"foo\": \"bar\"}", parsed.get("RETAIN_INNER_QUOTES"), "retains inner quotes");
  }

  @Test
  @DisplayName("retains inner quotes (as string)")
  void retainsInnerQuotesAsString() {
    assertEquals("{\"foo\": \"bar\"}", parsed.get("RETAIN_INNER_QUOTES_AS_STRING"),
        "retains inner quotes");
  }

  @Test
  @DisplayName("retains spaces in string")
  void retainsSpacesInString() {
    assertEquals("some spaced out string", parsed.get("TRIM_SPACE_FROM_UNQUOTED"),
        "retains spaces in string");
  }

  @Test
  @DisplayName("parses email addresses completely")
  void parsesEmailAddressesCompletely() {
    assertEquals("therealnerdybeast@example.tld", parsed.get("USERNAME"),
        "parses email addresses completely");
  }

  @Test
  @DisplayName("parses keys and values surrounded by spaces")
  void parsesKeysAndValuesSurroundedBySpaces() {
    assertEquals("parsed", parsed.get("SPACED_KEY"),
        "parses keys and values surrounded by spaces");
  }

  @Test
  @DisplayName("parses multi-line strings when using double quotes")
  void parsesMultiLineDoubleQuoted() {
    assertEquals("THIS\nIS\nA\nMULTILINE\nSTRING", parsed.get("MULTI_DOUBLE_QUOTED"),
        "parses multi-line strings when using double quotes");
  }

  @Test
  @DisplayName("parses multi-line strings when using single quotes")
  void parsesMultiLineSingleQuoted() {
    assertEquals("THIS\nIS\nA\nMULTILINE\nSTRING", parsed.get("MULTI_SINGLE_QUOTED"),
        "parses multi-line strings when using single quotes");
  }

  @Test
  @DisplayName("parses multi-line strings when using backticks")
  void parsesMultiLineBackticked() {
    assertEquals("THIS\nIS\nA\n\"MULTILINE'S\"\nSTRING", parsed.get("MULTI_BACKTICKED"),
        "parses multi-line strings when using backticks");
  }

  @Test
  @DisplayName("parses a multi-line PEM block")
  void parsesMultiLinePem() {
    String multiPem = String.join("\n",
        "-----BEGIN PUBLIC KEY-----",
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnNl1tL3QjKp3DZWM0T3u",
        "LgGJQwu9WqyzHKZ6WIA5T+7zPjO1L8l3S8k8YzBrfH4mqWOD1GBI8Yjq2L1ac3Y/",
        "bTdfHN8CmQr2iDJC0C6zY8YV93oZB3x0zC/LPbRYpF8f6OqX1lZj5vo2zJZy4fI/",
        "kKcI5jHYc8VJq+KCuRZrvn+3V+KuL9tF9v8ZgjF2PZbU+LsCy5Yqg1M8f5Jp5f6V",
        "u4QuUoobAgMBAAE=",
        "-----END PUBLIC KEY-----");
    assertEquals(multiPem, parsed.get("MULTI_PEM_DOUBLE_QUOTED"));
  }

  @Test
  @DisplayName("should parse a buffer into an object")
  void parsesABufferIntoAnObject() {
    Map<String, String> payload = Dotenv.parse("BUFFER=true".getBytes(StandardCharsets.UTF_8));
    assertEquals("true", payload.get("BUFFER"), "should parse a buffer into an object");
  }

  @Test
  @DisplayName("can parse (\\r) line endings")
  void parsesCarriageReturnLineEndings() {
    Map<String, String> payload =
        Dotenv.parse("SERVER=localhost\rPASSWORD=password\rDB=tests\r"
            .getBytes(StandardCharsets.UTF_8));
    assertEquals(expectedPayload(), payload, "can parse (\\r) line endings");
  }

  @Test
  @DisplayName("can parse (\\n) line endings")
  void parsesNewlineLineEndings() {
    Map<String, String> payload =
        Dotenv.parse("SERVER=localhost\nPASSWORD=password\nDB=tests\n"
            .getBytes(StandardCharsets.UTF_8));
    assertEquals(expectedPayload(), payload, "can parse (\\n) line endings");
  }

  @Test
  @DisplayName("can parse (\\r\\n) line endings")
  void parsesCarriageReturnNewlineLineEndings() {
    Map<String, String> payload =
        Dotenv.parse("SERVER=localhost\r\nPASSWORD=password\r\nDB=tests\r\n"
            .getBytes(StandardCharsets.UTF_8));
    assertEquals(expectedPayload(), payload, "can parse (\\r\\n) line endings");
  }

  private static Map<String, String> expectedPayload() {
    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("SERVER", "localhost");
    expected.put("PASSWORD", "password");
    expected.put("DB", "tests");
    return expected;
  }
}
