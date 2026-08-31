package io.github.motdotla.dotenv.node;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Decodes bytes the way {@code Buffer.prototype.toString(encoding)} does, so the
 * {@code encoding} config option keeps accepting the same names and producing the same
 * strings it did under Node.
 */
final class NodeEncoding {

  private NodeEncoding() {
  }

  static String decode(byte[] bytes, String encoding) {
    String name = encoding == null ? "utf8" : encoding.toLowerCase(Locale.ROOT);
    switch (name) {
      case "utf8":
      case "utf-8":
        return new String(bytes, StandardCharsets.UTF_8);
      case "utf16le":
      case "utf-16le":
      case "ucs2":
      case "ucs-2":
        return new String(bytes, StandardCharsets.UTF_16LE);
      case "latin1":
      case "binary":
        return new String(bytes, StandardCharsets.ISO_8859_1);
      case "ascii":
        return decodeAscii(bytes);
      case "base64":
        return Base64.getEncoder().encodeToString(bytes);
      case "base64url":
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
      case "hex":
        return HexFormat.of().formatHex(bytes);
      default:
        throw new IllegalArgumentException("Unknown encoding: " + encoding);
    }
  }

  /** Node's {@code ascii} encoding masks off the high bit rather than rejecting it. */
  private static String decodeAscii(byte[] bytes) {
    char[] chars = new char[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      chars[i] = (char) (bytes[i] & 0x7F);
    }
    return new String(chars);
  }
}
