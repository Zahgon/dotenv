package io.github.motdotla.dotenv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Covers the JavaScript semantics the port reproduces.
 *
 * <p>Under Node these came free from the language; here they are dotenv's own code, so they
 * need their own tests.
 */
class JsTest {

  @ParameterizedTest(name = "[{index}] \"{0}\" is off")
  @ValueSource(strings = {"false", "FALSE", "False", "0", "no", "NO", "off", "OFF", ""})
  @DisplayName("dotenv's falsy spellings turn an option off")
  void falsyStrings(String value) {
    assertFalse(Js.parseBoolean(value));
  }

  @ParameterizedTest(name = "[{index}] \"{0}\" is on")
  @ValueSource(strings = {"true", "TRUE", "1", "yes", "on", "anything", " ", "falsey"})
  @DisplayName("every other string turns an option on")
  void truthyStrings(String value) {
    assertTrue(Js.parseBoolean(value));
  }

  @Test
  @DisplayName("non-strings follow JavaScript's Boolean() coercion")
  void nonStrings() {
    assertFalse(Js.parseBoolean(null));
    assertFalse(Js.parseBoolean(false));
    assertTrue(Js.parseBoolean(true));
    assertFalse(Js.parseBoolean(0));
    assertFalse(Js.parseBoolean(0.0));
    assertFalse(Js.parseBoolean(Double.NaN));
    assertTrue(Js.parseBoolean(1));
    assertTrue(Js.parseBoolean(-1));
    assertTrue(Js.parseBoolean(new Object()));
  }

  /**
   * Exactly the 25 code units JavaScript's {@code \s} and {@code String.prototype.trim}
   * accept, enumerated from Node. Java's own {@code \s} differs on 19 of the 65,536 code
   * units, which is why the parser carries its own class.
   */
  private static final char[] JS_WHITESPACE = {
      0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x0020, 0x00A0, 0x1680, 0x2000, 0x2001, 0x2002,
      0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200A, 0x2028, 0x2029, 0x202F,
      0x205F, 0x3000, 0xFEFF};

  @Test
  @DisplayName("whitespace is exactly JavaScript's set, no more and no less")
  void whitespaceSet() {
    java.util.Set<Character> expected = new java.util.HashSet<>();
    for (char c : JS_WHITESPACE) {
      expected.add(c);
      assertTrue(Js.isWhitespace(c), "expected U+" + Integer.toHexString(c) + " to be whitespace");
    }
    for (int c = 0; c <= 0xFFFF; c++) {
      assertEquals(expected.contains((char) c), Js.isWhitespace((char) c),
          "disagreed with JavaScript on U+" + Integer.toHexString(c));
    }
  }

  @Test
  @DisplayName("U+0085 is not whitespace, though Java's regex engine treats it as a line break")
  void nextLineIsNotWhitespace() {
    // the character behind one of the two engine bugs this port had to fix
    assertFalse(Js.isWhitespace((char) 0x0085));
    assertFalse(Js.isWhitespace((char) 0x200B));
    assertFalse(Js.isWhitespace((char) 0x2060));
  }

  @Test
  @DisplayName("trim strips the BOM and non-breaking space, which String.strip does not")
  void trimStripsMoreThanStrip() {
    assertEquals("value", Js.trim("﻿ value  "));
    assertEquals("value", Js.trim(" value　"));
    assertEquals("", Js.trim("﻿  \t\n"));
    assertEquals("a  b", Js.trim("  a  b  "));
    assertEquals("", Js.trim(""));
  }
}
