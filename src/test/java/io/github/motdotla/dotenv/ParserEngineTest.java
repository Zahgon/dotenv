package io.github.motdotla.dotenv;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Pins the two places Java's regex engine does not behave like JavaScript's.
 *
 * <p>Both were live defects in this port before they were found by differential testing, and
 * neither is covered by the tests carried over from the original — under Node the engine
 * simply did the right thing. Every expectation below is what the original JavaScript
 * actually produces for the same input.
 */
class ParserEngineTest {

  private static final String NEL = Character.toString(0x0085);
  private static final String LS = Character.toString(0x2028);
  private static final String PS = Character.toString(0x2029);

  private static Map<String, String> classic(String src) {
    return Dotenv.parse(src);
  }

  private static Map<String, String> fast(String src) {
    return Dotenv.parse(src, new ParseOptions().fast(true));
  }

  private static Map<String, String> map(String... kv) {
    Map<String, String> m = new LinkedHashMap<>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put(kv[i], kv[i + 1]);
    }
    return m;
  }

  /**
   * Java counts U+0085 (NEL) as a line terminator for {@code ^}, {@code $} and {@code .};
   * JavaScript does not. Relying on {@code Pattern.MULTILINE} made a key after a stray NEL
   * parse here when it does not parse in the original.
   */
  @Nested
  @DisplayName("U+0085 is not a line terminator")
  class NextLine {

    @Test
    @DisplayName("a key behind a NEL is not reachable")
    void keyBehindNel() {
      assertEquals(Map.of(), classic(NEL + "A=b"));
      assertEquals(Map.of(), fast(NEL + "A=b"));
    }

    @Test
    @DisplayName("a NEL does not end a value")
    void nelDoesNotEndAValue() {
      assertEquals(map("A", "b" + NEL + "C=d"), classic("A=b" + NEL + "C=d"));
      assertEquals(map("A", "b" + NEL + "C=d"), fast("A=b" + NEL + "C=d"));
    }

    @Test
    @DisplayName("a NEL does not end a comment")
    void nelDoesNotEndAComment() {
      assertEquals(Map.of(), classic("# comment" + NEL + "B=2"));
      assertEquals(Map.of(), fast("# comment" + NEL + "B=2"));
    }

    @Test
    @DisplayName("a NEL does not separate export from its key")
    void nelAfterExport() {
      assertEquals(Map.of(), classic("export" + NEL + "A=b"));
      assertEquals(Map.of(), fast("export" + NEL + "A=b"));
    }

    @Test
    @DisplayName("a NEL inside a value is kept")
    void nelInsideValue() {
      assertEquals(map("A", "b" + NEL + "c"), classic("A=b" + NEL + "c"));
      assertEquals(map("A", "b" + NEL + "c"), fast("A=b" + NEL + "c"));
    }
  }

  /** U+2028 and U+2029 are line terminators to JavaScript, and must stay that way here. */
  @Nested
  @DisplayName("U+2028 and U+2029 keep their JavaScript meaning")
  class ParagraphSeparators {

    @Test
    @DisplayName("a line separator stays inside an unquoted value")
    void lineSeparatorInsideValue() {
      assertEquals(map("A", "b" + LS + "C=d"), classic("A=b" + LS + "C=d"));
      assertEquals(map("A", "b" + LS + "C=d"), fast("A=b" + LS + "C=d"));
    }

    @Test
    @DisplayName("a paragraph separator stays inside an unquoted value")
    void paragraphSeparatorInsideValue() {
      assertEquals(map("A", "b" + PS + "C=d"), classic("A=b" + PS + "C=d"));
      assertEquals(map("A", "b" + PS + "C=d"), fast("A=b" + PS + "C=d"));
    }

    @Test
    @DisplayName("the classic parser skips a leading line separator and the fast one does not")
    void parsersDisagreeOnLeadingLineSeparator() {
      // the two parsers genuinely differ here in the original, because the classic parser
      // skips it as part of \s* and the scanner only skips space, tab, newline and the BOM
      assertEquals(map("A", "b"), classic(LS + "A=b"));
      assertEquals(Map.of(), fast(LS + "A=b"));
    }
  }

  /**
   * Java compiles an alternation inside a greedy loop into a matcher that costs a stack
   * frame per character, so the original spelling of the quoted-value group threw
   * {@link StackOverflowError} at a few thousand characters — well within the size of the
   * private keys and certificates multiline values exist for.
   */
  @Nested
  @DisplayName("large quoted values do not exhaust the stack")
  class LargeValues {

    @Test
    @DisplayName("a 100k double-quoted value round-trips")
    void hugeDoubleQuoted() {
      String value = "x".repeat(100_000);
      Map<String, String> parsed =
          assertDoesNotThrow(() -> classic("A=\"" + value + "\""));
      assertEquals(100_000, parsed.get("A").length());
    }

    @Test
    @DisplayName("a 100k single-quoted value round-trips")
    void hugeSingleQuoted() {
      String value = "x".repeat(100_000);
      Map<String, String> parsed = assertDoesNotThrow(() -> classic("A='" + value + "'"));
      assertEquals(100_000, parsed.get("A").length());
    }

    @Test
    @DisplayName("a 100k backtick value round-trips")
    void hugeBackticked() {
      String value = "x".repeat(100_000);
      Map<String, String> parsed = assertDoesNotThrow(() -> classic("A=`" + value + "`"));
      assertEquals(100_000, parsed.get("A").length());
    }

    @Test
    @DisplayName("a 55k multiline value round-trips, as a large private key would")
    void hugeMultiline() {
      String body = "abcdefghij\n".repeat(5_000);
      Map<String, String> parsed = assertDoesNotThrow(() -> classic("K=\"" + body + "\""));
      assertEquals(55_000, parsed.get("K").length());
    }

    @Test
    @DisplayName("a 100k unquoted value round-trips")
    void hugeUnquoted() {
      String value = "x".repeat(100_000);
      Map<String, String> parsed = assertDoesNotThrow(() -> classic("A=" + value));
      assertEquals(100_000, parsed.get("A").length());
    }

    @Test
    @DisplayName("the fast parser agrees on all of them")
    void fastParserAgrees() {
      for (String src : new String[] {
          "A=\"" + "x".repeat(100_000) + "\"",
          "A='" + "x".repeat(100_000) + "'",
          "A=`" + "x".repeat(100_000) + "`",
          "K=\"" + "abcdefghij\n".repeat(5_000) + "\"",
          "A=" + "x".repeat(100_000)}) {
        assertEquals(classic(src), fast(src));
      }
    }

    @Test
    @DisplayName("a file of many lines parses")
    void manyLines() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 20_000; i++) {
        sb.append("K").append(i).append("=v").append(i).append('\n');
      }
      String src = sb.toString();
      Map<String, String> parsed = assertDoesNotThrow(() -> classic(src));
      assertEquals(20_000, parsed.size());
      assertEquals("v19999", parsed.get("K19999"));
    }
  }
}
