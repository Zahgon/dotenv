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

/** Ported from {@code tests/test-parse.js}. */
class ParseTest {

  private static Map<String, String> parsed;

  @BeforeAll
  static void parseFixture() {
    parsed = Dotenv.parse(NodeFs.readFileSync("tests/.env", "utf8"));
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
  @DisplayName("defaults empty values to empty string (single quotes)")
  void defaultsEmptySingleQuotesToEmptyString() {
    assertEquals("", parsed.get("EMPTY_SINGLE_QUOTES"), "defaults empty values to empty string");
  }

  @Test
  @DisplayName("defaults empty values to empty string (double quotes)")
  void defaultsEmptyDoubleQuotesToEmptyString() {
    assertEquals("", parsed.get("EMPTY_DOUBLE_QUOTES"), "defaults empty values to empty string");
  }

  @Test
  @DisplayName("defaults empty values to empty string (backticks)")
  void defaultsEmptyBackticksToEmptyString() {
    assertEquals("", parsed.get("EMPTY_BACKTICKS"), "defaults empty values to empty string");
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
  @DisplayName("respects double quotes inside single quotes")
  void respectsDoubleQuotesInsideSingleQuotes() {
    assertEquals("double \"quotes\" work inside single quotes",
        parsed.get("DOUBLE_QUOTES_INSIDE_SINGLE"), "respects double quotes inside single quotes");
  }

  @Test
  @DisplayName("respects spacing for badly formed brackets")
  void respectsSpacingForBadlyFormedBrackets() {
    assertEquals("{ port: $MONGOLAB_PORT}", parsed.get("DOUBLE_QUOTES_WITH_NO_SPACE_BRACKET"),
        "respects spacing for badly formed brackets");
  }

  @Test
  @DisplayName("respects single quotes inside double quotes")
  void respectsSingleQuotesInsideDoubleQuotes() {
    assertEquals("single 'quotes' work inside double quotes",
        parsed.get("SINGLE_QUOTES_INSIDE_DOUBLE"), "respects single quotes inside double quotes");
  }

  @Test
  @DisplayName("respects backticks inside single quotes")
  void respectsBackticksInsideSingleQuotes() {
    assertEquals("`backticks` work inside single quotes", parsed.get("BACKTICKS_INSIDE_SINGLE"),
        "respects backticks inside single quotes");
  }

  @Test
  @DisplayName("respects backticks inside double quotes")
  void respectsBackticksInsideDoubleQuotes() {
    assertEquals("`backticks` work inside double quotes", parsed.get("BACKTICKS_INSIDE_DOUBLE"),
        "respects backticks inside double quotes");
  }

  @Test
  @DisplayName("BACKTICKS")
  void backticks() {
    assertEquals("backticks", parsed.get("BACKTICKS"));
  }

  @Test
  @DisplayName("BACKTICKS_SPACED")
  void backticksSpaced() {
    assertEquals("    backticks    ", parsed.get("BACKTICKS_SPACED"));
  }

  @Test
  @DisplayName("respects double quotes inside backticks")
  void respectsDoubleQuotesInsideBackticks() {
    assertEquals("double \"quotes\" work inside backticks",
        parsed.get("DOUBLE_QUOTES_INSIDE_BACKTICKS"), "respects double quotes inside backticks");
  }

  @Test
  @DisplayName("respects single quotes inside backticks")
  void respectsSingleQuotesInsideBackticks() {
    assertEquals("single 'quotes' work inside backticks",
        parsed.get("SINGLE_QUOTES_INSIDE_BACKTICKS"), "respects single quotes inside backticks");
  }

  @Test
  @DisplayName("respects double and single quotes inside backticks")
  void respectsDoubleAndSingleQuotesInsideBackticks() {
    assertEquals("double \"quotes\" and single 'quotes' work inside backticks",
        parsed.get("DOUBLE_AND_SINGLE_QUOTES_INSIDE_BACKTICKS"),
        "respects single quotes inside backticks");
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
  @DisplayName("ignores inline comments")
  void ignoresInlineComments() {
    assertEquals("inline comments", parsed.get("INLINE_COMMENTS"), "ignores inline comments");
  }

  @Test
  @DisplayName("ignores inline comments and respects # character inside of single quotes")
  void ignoresInlineCommentsInsideSingleQuotes() {
    assertEquals("inline comments outside of #singlequotes",
        parsed.get("INLINE_COMMENTS_SINGLE_QUOTES"),
        "ignores inline comments and respects # character inside of single quotes");
  }

  @Test
  @DisplayName("ignores inline comments and respects # character inside of double quotes")
  void ignoresInlineCommentsInsideDoubleQuotes() {
    assertEquals("inline comments outside of #doublequotes",
        parsed.get("INLINE_COMMENTS_DOUBLE_QUOTES"),
        "ignores inline comments and respects # character inside of double quotes");
  }

  @Test
  @DisplayName("ignores inline comments and respects # character inside of backticks")
  void ignoresInlineCommentsInsideBackticks() {
    assertEquals("inline comments outside of #backticks", parsed.get("INLINE_COMMENTS_BACKTICKS"),
        "ignores inline comments and respects # character inside of backticks");
  }

  @Test
  @DisplayName("treats # character as start of comment")
  void treatsHashAsStartOfComment() {
    assertEquals("inline comments start with a", parsed.get("INLINE_COMMENTS_SPACE"),
        "treats # character as start of comment");
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
  @DisplayName("retains inner quotes (as backticks)")
  void retainsInnerQuotesAsBackticks() {
    assertEquals("{\"foo\": \"bar's\"}", parsed.get("RETAIN_INNER_QUOTES_AS_BACKTICKS"),
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
  @DisplayName("should parse a buffer into an object")
  void parsesABufferIntoAnObject() {
    Map<String, String> payload = Dotenv.parse("BUFFER=true".getBytes(StandardCharsets.UTF_8));
    assertEquals("true", payload.get("BUFFER"), "should parse a buffer into an object");
  }

  @Test
  @DisplayName("last duplicate key wins")
  void lastDuplicateKeyWins() {
    Map<String, String> duplicate =
        Dotenv.parse("DUP=one\nDUP=two".getBytes(StandardCharsets.UTF_8));
    assertEquals("two", duplicate.get("DUP"), "last duplicate key wins");
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

  @Test
  @DisplayName("ignores export keyword")
  void ignoresExportKeyword() {
    assertEquals("parsed", parsed.get("EXPORT_IS_DECLARED"), "ignores export keyword");
  }

  @Test
  @DisplayName("ignores export keyword and spacing")
  void ignoresExportKeywordAndSpacing() {
    assertEquals("parsed", parsed.get("EXPORT_IS_DECLARED_WITH_SPACING"),
        "ignores export keyword and spacing");
  }

  @Test
  @DisplayName("ignores export keyword and parses value")
  void ignoresExportKeywordAndParsesValue() {
    assertEquals("some_value", parsed.get("EXPORT_IS_DECLARED_WITH_SOME_VALUE"),
        "ignores export keyword and parses value");
  }

  @Test
  @DisplayName("ignores export keyword and parses value with spacing")
  void ignoresExportKeywordAndParsesValueSpaced() {
    assertEquals("some_value", parsed.get("EXPORT_IS_DECLARED_WITH_SOME_VALUE_SPACED"),
        "ignores export keyword and parses value with spacing");
  }

  @Test
  @DisplayName("ignores export keyword and parses value with spacing (and spacing)")
  void ignoresExportKeywordAndParsesValueAndSpacing() {
    assertEquals("some_value", parsed.get("EXPORT_IS_DECLARED_WITH_SOME_VALUE_AND_SPACING"),
        "ignores export keyword and parses value with spacing");
  }

  private static Map<String, String> expectedPayload() {
    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("SERVER", "localhost");
    expected.put("PASSWORD", "password");
    expected.put("DB", "tests");
    return expected;
  }
}
