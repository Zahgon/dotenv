package io.github.motdotla.dotenv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.motdotla.dotenv.node.NodeFs;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Ported from {@code tests/test-parse-fast.js}. */
class ParseFastTest {

  private static void assertSameParse(String src) {
    Map<String, String> classic = Dotenv.parse(src);
    Map<String, String> fast = Dotenv.parse(src, new ParseOptions().fast(true));
    assertEquals(classic, fast, "fast parse matches classic parse");
  }

  @Test
  @DisplayName("fast parse matches classic parse for tests/.env")
  void matchesClassicForEnvFixture() {
    assertSameParse(NodeFs.readFileSync("tests/.env", "utf8"));
  }

  @Test
  @DisplayName("fast parse matches classic parse for multiline fixture")
  void matchesClassicForMultilineFixture() {
    assertSameParse(NodeFs.readFileSync("tests/.env.multiline", "utf8"));
  }

  static Stream<String> edgeCases() {
    return Stream.of(
        "BASIC=basic",
        "export KEY=value",
        "KEY: value",
        "EMPTY=",
        "SINGLE='single'",
        "DOUBLE=\"double\"",
        "BACKTICK=`backtick`",
        "DOUBLE=\"line one\\nline two\"",
        "INLINE=value # comment",
        "HASH=\"value#notcomment\"",
        "EQUALS==value",
        "# comment only\n",
        "",
        "KEY=val\r\nOTHER=ok\r",
        "MULTI=\"one\ntwo\"",
        "ESCAPED=\"say \\\"hi\\\"\"");
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("edgeCases")
  @DisplayName("fast parse matches classic parse for edge cases")
  void matchesClassicForEdgeCases(String src) {
    assertSameParse(src);
  }

  static Stream<String> bomCases() {
    return Stream.of(
        "\uFEFFBASIC=basic",
        "\uFEFFBASIC=basic\nSECOND=two\n",
        "\uFEFFexport BASIC=basic\n",
        "\uFEFF# comment first\nBASIC=basic\n",
        "\uFEFF",
        "\n\uFEFFBASIC=basic\n",
        "FIRST=one\n\uFEFFSECOND=two\n");
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("bomCases")
  @DisplayName("fast parse matches classic parse for a leading UTF-8 BOM")
  void matchesClassicForLeadingBom(String src) {
    assertSameParse(src);
  }

  @Test
  @DisplayName("pins the leading-BOM behaviour")
  void pinsLeadingBomBehaviour() {
    // pin the behaviour, so the two parsers can't agree by both being wrong
    assertEquals(Map.of("BASIC", "basic"),
        Dotenv.parse("\uFEFFBASIC=basic", new ParseOptions().fast(true)));
  }

  static Stream<String> escapedBackslashCases() {
    return Stream.of(
        "KEY=\"\\\\\"",
        "KEY=\"\\\\\"\nNEXT=ok\n",
        "KEY='\\\\'\nNEXT=ok\n",
        "KEY=`\\\\`\nNEXT=ok\n",
        "WINDIR=\"C:\\\\Users\\\\me\\\\\"\nAPI_KEY=secret\nPORT=3000\n",
        "KEY=\"a\\\\b\"\nNEXT=ok\n",
        "KEY=\"\\\\\\\\\"\nNEXT=ok\n",
        "A=\"\\\\\"\nB=plain\nC=\"quoted\"\nD=last\n");
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("escapedBackslashCases")
  @DisplayName("fast parse matches classic parse for an escaped backslash before the closing quote")
  void matchesClassicForEscapedBackslash(String src) {
    assertSameParse(src);
  }

  @Test
  @DisplayName("pins the escaped-backslash behaviour")
  void pinsEscapedBackslashBehaviour() {
    // pin the behaviour, so the two parsers can't agree by both being wrong.
    // dotenv does not unescape \\, so the value keeps both backslashes — the point
    // is that the value ends at its own closing quote and the later keys survive.
    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("A", "\\\\");
    expected.put("B", "plain");
    expected.put("C", "quoted");
    expected.put("D", "last");
    assertEquals(expected,
        Dotenv.parse("A=\"\\\\\"\nB=plain\nC=\"quoted\"\nD=last\n",
            new ParseOptions().fast(true)));
  }

  @Test
  @DisplayName("config with fast reads a .env written with a BOM, populating processEnv")
  void fastConfigReadsBomFilePopulatingProcessEnv() {
    Map<String, String> processEnv = new LinkedHashMap<>();
    Dotenv.config(new ConfigOptions()
        .path("tests/.env.bom").quiet(true).fast(true).processEnv(processEnv));

    assertEquals("basic", processEnv.get("BASIC"));
  }

  @Test
  @DisplayName("config with fast reads a .env written with a BOM, returning parsed")
  void fastConfigReadsBomFileReturningParsed() {
    Map<String, String> processEnv = new LinkedHashMap<>();
    ConfigResult result = Dotenv.config(new ConfigOptions()
        .path("tests/.env.bom").quiet(true).fast(true).processEnv(processEnv));

    assertEquals("basic", result.parsed().get("BASIC"));
  }

  @Test
  @DisplayName("config with fast loads with the fast parser, populating processEnv")
  void fastConfigLoadsPopulatingProcessEnv() {
    Map<String, String> processEnv = new LinkedHashMap<>();
    Dotenv.config(new ConfigOptions()
        .path("tests/.env").quiet(true).fast(true).processEnv(processEnv));

    assertEquals("basic", processEnv.get("BASIC"));
  }

  @Test
  @DisplayName("config with fast loads with the fast parser, returning parsed")
  void fastConfigLoadsReturningParsed() {
    Map<String, String> processEnv = new LinkedHashMap<>();
    ConfigResult result = Dotenv.config(new ConfigOptions()
        .path("tests/.env").quiet(true).fast(true).processEnv(processEnv));

    assertEquals("basic", result.parsed().get("BASIC"));
  }
}
