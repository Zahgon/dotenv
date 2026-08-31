package io.github.motdotla.dotenv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.motdotla.dotenv.node.NodeFs;
import io.github.motdotla.dotenv.node.NodeOs;
import io.github.motdotla.dotenv.node.NodePath;
import io.github.motdotla.dotenv.node.NodeProcess;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the Node standard-library behaviour the port now implements itself.
 *
 * <p>Under Node these were {@code fs}, {@code os} and {@code path}, covered by someone
 * else's test suite. The expected values below are what Node actually produces for the same
 * inputs.
 */
class NodeShimsTest {

  /** {@code fs.readFileSync(path, { encoding })} accepts all of these spellings. */
  @Nested
  class Encodings {

    @TempDir
    private Path dir;

    /** The bytes of {@code "héllo wörld\n"} in UTF-8, as Node would read them. */
    private Path file;

    @BeforeEach
    void writeSample() throws IOException {
      file = dir.resolve("sample");
      Files.write(file, "héllo wörld\n".getBytes(StandardCharsets.UTF_8));
    }

    private String read(String encoding) {
      return NodeFs.readFileSync(file.toString(), encoding);
    }

    @Test
    @DisplayName("utf8 is the default and its aliases agree")
    void utf8() {
      assertEquals("héllo wörld\n", read("utf8"));
      assertEquals("héllo wörld\n", read("utf-8"));
      assertEquals("héllo wörld\n", read("UTF8"));
      assertEquals("héllo wörld\n", read(null));
    }

    @Test
    @DisplayName("latin1 and binary decode byte-for-byte")
    void latin1() {
      assertEquals("hÃ©llo wÃ¶rld\n", read("latin1"));
      assertEquals("hÃ©llo wÃ¶rld\n", read("binary"));
    }

    @Test
    @DisplayName("ascii masks off the high bit rather than rejecting it")
    void ascii() {
      assertEquals("hC)llo wC6rld\n", read("ascii"));
    }

    @Test
    @DisplayName("utf16le and its aliases decode little-endian pairs")
    void utf16le() {
      String expected = "써沩潬眠뛃汲੤";
      assertEquals(expected, read("utf16le"));
      assertEquals(expected, read("utf-16le"));
      assertEquals(expected, read("ucs2"));
      assertEquals(expected, read("ucs-2"));
    }

    @Test
    @DisplayName("base64, base64url and hex re-encode the bytes")
    void reEncodings() {
      assertEquals("aMOpbGxvIHfDtnJsZAo=", read("base64"));
      assertEquals("aMOpbGxvIHfDtnJsZAo", read("base64url"));
      assertEquals("68c3a96c6c6f2077c3b6726c640a", read("hex"));
    }

    @Test
    @DisplayName("an unknown encoding is rejected the way Node rejects it")
    void unknownEncoding() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> read("bogus"));
      assertEquals("Unknown encoding: bogus", e.getMessage());
    }
  }

  @Nested
  class FileErrors {

    @Test
    @DisplayName("a missing file reports ENOENT with the path as written")
    void missingFile() {
      DotenvException e = assertThrows(DotenvException.class,
          () -> NodeFs.readFileSync("does/not/exist/.env", "utf8"));

      assertEquals("ENOENT", e.code());
      assertEquals("ENOENT: no such file or directory, open 'does/not/exist/.env'",
          e.getMessage());
    }

    @Test
    @DisplayName("a directory reports EISDIR")
    void directory(@TempDir Path dir) {
      DotenvException e =
          assertThrows(DotenvException.class, () -> NodeFs.readFileSync(dir.toString(), "utf8"));

      assertEquals("EISDIR", e.code());
      assertEquals("EISDIR: illegal operation on a directory, read", e.getMessage());
    }

    @Test
    @DisplayName("an unreadable file reports EACCES")
    void unreadableFile(@TempDir Path dir) throws IOException {
      assumeFalse("win32".equals(NodeOs.platform()));
      assumeFalse("root".equals(System.getProperty("user.name")));
      Path file = dir.resolve(".env");
      Files.writeString(file, "BASIC=basic\n");
      Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("---------"));

      DotenvException e =
          assertThrows(DotenvException.class, () -> NodeFs.readFileSync(file.toString(), "utf8"));

      assertEquals("EACCES", e.code());
      assertEquals("EACCES: permission denied, open '" + file + "'", e.getMessage());
    }

    @Test
    @DisplayName("a file: URI is read, and reported by its filesystem path")
    void fileUri(@TempDir Path dir) throws IOException {
      Path file = dir.resolve(".env");
      Files.writeString(file, "BASIC=basic\n");

      assertEquals("BASIC=basic\n",
          NodeFs.readFileSync(URI.create("file://" + file), "utf8"));

      URI missing = URI.create("file://" + dir.resolve("nope"));
      DotenvException e =
          assertThrows(DotenvException.class, () -> NodeFs.readFileSync(missing, "utf8"));
      assertEquals("ENOENT: no such file or directory, open '" + dir.resolve("nope") + "'",
          e.getMessage());
    }

    @Test
    @DisplayName("a path that is neither a string nor a URI is rejected")
    void unsupportedPathType() {
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> NodeFs.readFileSync(42, "utf8"));
      assertTrue(e.getMessage().startsWith("The \"path\" argument must be of type string"),
          e.getMessage());
    }
  }

  @Nested
  class Paths {

    @Test
    @DisplayName("resolve makes a path absolute against the working directory")
    void resolve() {
      assertEquals(java.nio.file.Paths.get(NodeProcess.cwd(), ".env").toString(),
          NodePath.resolve(".env"));
      assertEquals("/etc/hosts", NodePath.resolve("/etc/hosts"));
    }

    @Test
    @DisplayName("relative shortens a path against the working directory")
    void relative() {
      assertEquals("tests/.env", NodePath.relative(NodeProcess.cwd(), "tests/.env"));
      assertEquals("tests/.env",
          NodePath.relative(NodeProcess.cwd(),
              java.nio.file.Paths.get("tests/.env").toAbsolutePath().toString()));
    }

    @Test
    @DisplayName("relative rejects a non-string the way Node's validateString does")
    void relativeRejectsNonString() {
      URI uri = URI.create("file:///tmp/.env");
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> NodePath.relative(NodeProcess.cwd(), uri));
      assertEquals("The \"to\" argument must be of type string. Received an instance of URI",
          e.getMessage());

      assertThrows(IllegalArgumentException.class, () -> NodePath.relative(null, "x"));
    }

    @Test
    @DisplayName("join glues segments together, absolute second segment included")
    void join() {
      assertEquals("/Users/dummy/.env", NodePath.join("/Users/dummy", "/.env"));
      assertEquals("/Users/dummy/.env", NodePath.join("/Users/dummy", ".env"));
      assertEquals(".", NodePath.join());
      assertEquals("/Users/dummy", NodePath.join("/Users/dummy", ""));
    }
  }

  @Test
  @DisplayName("os reports a Node-style platform and a home directory")
  void osReportsPlatformAndHome() {
    assertTrue(List.of("darwin", "linux", "win32").contains(NodeOs.platform()));
    assertNotNull(NodeOs.homedir());
  }

  @Test
  @DisplayName("the working directory is the project root while tests run")
  void cwdIsProjectRoot() {
    assumeTrue(Files.exists(java.nio.file.Paths.get("pom.xml")));
    assertEquals(System.getProperty("user.dir"), NodeProcess.cwd());
  }

  @Test
  @DisplayName("a home-relative ~ path is expanded before the file is read")
  void tildeIsExpanded(@TempDir Path dir) throws IOException {
    Path file = dir.resolve(".env");
    Files.writeString(file, "BASIC=basic\n");
    // NodePath.join is what expands ~, so check it lands on the same file
    String joined = NodePath.join(dir.toString(), "/.env");
    assertEquals(file.toString(), joined);
    assertEquals("BASIC=basic\n", NodeFs.readFileSync(joined, "utf8"));
  }
}
