package io.github.motdotla.dotenv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the JSON reading the port now does itself.
 *
 * <p>Under Node this was the JSON parser's job; here it is dotenv's, so it needs its own
 * tests.
 */
class JsonTest {

  @Test
  @DisplayName("reads a flat object of string values")
  void readsFlatObject() {
    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("HELLO", "World");
    expected.put("PORT", "3000");
    assertEquals(expected, Json.parseObject("{\"HELLO\":\"World\",\"PORT\":\"3000\"}"));
  }

  @Test
  @DisplayName("reads an empty object")
  void readsEmptyObject() {
    assertEquals(Map.of(), Json.parseObject("{}"));
    assertEquals(Map.of(), Json.parseObject("  {  }  "));
  }

  @Test
  @DisplayName("preserves key order")
  void preservesKeyOrder() {
    Map<String, String> parsed = Json.parseObject("{\"c\":\"3\",\"a\":\"1\",\"b\":\"2\"}");
    assertEquals(List.of("c", "a", "b"), List.copyOf(parsed.keySet()));
  }

  @Test
  @DisplayName("decodes string escapes")
  void decodesStringEscapes() {
    Map<String, String> parsed = Json.parseObject(
        "{\"A\":\"line\\nbreak\",\"B\":\"quote\\\"inside\",\"C\":\"back\\\\slash\","
            + "\"D\":\"tab\\there\",\"E\":\"\\u00e9\\u65e5\"}");
    assertEquals("line\nbreak", parsed.get("A"));
    assertEquals("quote\"inside", parsed.get("B"));
    assertEquals("back\\slash", parsed.get("C"));
    assertEquals("tab\there", parsed.get("D"));
    assertEquals("é日", parsed.get("E"));
  }

  @Test
  @DisplayName("renders scalar values as their text")
  void rendersScalarsAsText() {
    Map<String, String> parsed =
        Json.parseObject("{\"N\":42,\"F\":-1.5e3,\"T\":true,\"B\":false}");
    assertEquals("42", parsed.get("N"));
    assertEquals("-1.5e3", parsed.get("F"));
    assertEquals("true", parsed.get("T"));
    assertEquals("false", parsed.get("B"));
  }

  @Test
  @DisplayName("skips null and nested containers, which are not environment values")
  void skipsNullAndContainers() {
    Map<String, String> parsed = Json.parseObject(
        "{\"A\":null,\"B\":{\"nested\":\"x\"},\"C\":[1,2,{\"d\":\"}\"}],\"D\":\"kept\"}");
    assertEquals(Map.of("D", "kept"), parsed);
  }

  @Test
  @DisplayName("skips a nested container containing braces inside strings")
  void skipsContainerWithBracesInStrings() {
    Map<String, String> parsed =
        Json.parseObject("{\"A\":{\"x\":\"}{\"},\"B\":\"kept\"}");
    assertEquals(Map.of("B", "kept"), parsed);
  }

  @Test
  @DisplayName("tolerates whitespace between tokens")
  void toleratesWhitespace() {
    assertEquals(Map.of("A", "1"), Json.parseObject("{\n  \"A\" : \"1\"\n}\n"));
  }

  @Test
  @DisplayName("rejects malformed input")
  void rejectsMalformedInput() {
    for (String bad : List.of("", "{", "{\"A\"}", "{\"A\":}", "{\"A\":\"1\",}",
        "{\"A\":\"1\"} trailing", "[]", "{\"A\":\"unterminated}")) {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> Json.parseObject(bad),
              "expected to reject: " + bad);
      assertTrue(e.getMessage().startsWith("invalid JSON"), e.getMessage());
    }
  }

  @Test
  @DisplayName("rejects an invalid escape")
  void rejectsInvalidEscape() {
    assertThrows(IllegalArgumentException.class, () -> Json.parseObject("{\"A\":\"\\q\"}"));
    assertThrows(IllegalArgumentException.class, () -> Json.parseObject("{\"A\":\"\\uZZZZ\"}"));
  }
}
