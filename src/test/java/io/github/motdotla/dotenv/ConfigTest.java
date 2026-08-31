package io.github.motdotla.dotenv;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import io.github.motdotla.dotenv.node.NodeFs;
import io.github.motdotla.dotenv.node.NodeOs;
import io.github.motdotla.dotenv.node.NodePath;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/** Ported from {@code tests/test-config.js}. */
class ConfigTest {

  private static final List<String> CONFIG_ENV_KEYS = List.of(
      "DOTENV_CONFIG_ENCODING",
      "DOTENV_CONFIG_PATH",
      "DOTENV_CONFIG_QUIET",
      "DOTENV_CONFIG_DEBUG",
      "DOTENV_CONFIG_OVERRIDE",
      "DOTENV_CONFIG_SECURE",
      "DOTENV_CONFIG_FAST");

  @BeforeEach
  void reset() {
    Dotenv.processEnv().remove("BASIC"); // reset
  }

  @AfterEach
  void clearConfigEnv() {
    CONFIG_ENV_KEYS.forEach(Dotenv.processEnv()::remove);
  }

  @Test
  @DisplayName("uses DOTENV_CONFIG_* values as config defaults")
  void usesConfigEnvDefaults() {
    Dotenv.processEnv().put("DOTENV_CONFIG_PATH", "tests/.env.local");
    Dotenv.processEnv().put("DOTENV_CONFIG_QUIET", "true");
    Dotenv.processEnv().put("DOTENV_CONFIG_OVERRIDE", "true");
    Map<String, String> processEnv = new LinkedHashMap<>(Map.of("BASIC", "existing"));

    boolean loggedErr;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.config(new ConfigOptions().processEnv(processEnv));
      loggedErr = console.loggedErr();
    }

    assertAll(
        () -> assertEquals("local_basic", processEnv.get("BASIC")),
        () -> assertFalse(loggedErr));
  }

  @Test
  @DisplayName("config options override DOTENV_CONFIG_* defaults")
  void configOptionsOverrideConfigEnvDefaults() {
    Dotenv.processEnv().put("DOTENV_CONFIG_PATH", "tests/.env.local");
    Dotenv.processEnv().put("DOTENV_CONFIG_QUIET", "true");
    Dotenv.processEnv().put("DOTENV_CONFIG_OVERRIDE", "true");
    Map<String, String> processEnv = new LinkedHashMap<>(Map.of("BASIC", "existing"));

    boolean loggedErr;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.config(new ConfigOptions()
          .path("tests/.env").quiet(false).override(false).processEnv(processEnv));
      loggedErr = console.loggedErr();
    }

    assertAll(
        () -> assertEquals("existing", processEnv.get("BASIC")),
        () -> assertTrue(loggedErr));
  }

  @Test
  @DisplayName("takes string for path option")
  void takesStringForPathOption() {
    ConfigResult env = quietly(() -> Dotenv.config(new ConfigOptions().path("tests/.env")));

    assertAll(
        () -> assertEquals("basic", env.parsed().get("BASIC")),
        () -> assertEquals("basic", Dotenv.processEnv().get("BASIC")));
  }

  @Test
  @DisplayName("takes array for path option")
  void takesArrayForPathOption() {
    ConfigResult env =
        quietly(() -> Dotenv.config(new ConfigOptions().path(List.of("tests/.env"))));

    assertAll(
        () -> assertEquals("basic", env.parsed().get("BASIC")),
        () -> assertEquals("basic", Dotenv.processEnv().get("BASIC")));
  }

  @Test
  @DisplayName("takes two or more files in the array for path option")
  void takesTwoOrMoreFilesForPathOption() {
    ConfigResult env = quietly(() ->
        Dotenv.config(new ConfigOptions().path(List.of("tests/.env.local", "tests/.env"))));

    assertAll(
        () -> assertEquals("local_basic", env.parsed().get("BASIC")),
        () -> assertEquals("local_basic", Dotenv.processEnv().get("BASIC")));
  }

  @Test
  @DisplayName("sets values from both .env.local and .env. first file key wins.")
  void firstFileKeyWins() {
    Dotenv.processEnv().remove("SINGLE_QUOTES");

    ConfigResult env = quietly(() ->
        Dotenv.config(new ConfigOptions().path(List.of("tests/.env.local", "tests/.env"))));

    assertAll(
        // in both files - first file wins (.env.local)
        () -> assertEquals("local_basic", env.parsed().get("BASIC")),
        () -> assertEquals("local_basic", Dotenv.processEnv().get("BASIC")),
        // in .env.local only
        () -> assertEquals("local", env.parsed().get("LOCAL")),
        () -> assertEquals("local", Dotenv.processEnv().get("LOCAL")),
        // in .env only
        () -> assertEquals("single_quotes", env.parsed().get("SINGLE_QUOTES")),
        () -> assertEquals("single_quotes", Dotenv.processEnv().get("SINGLE_QUOTES")));
  }

  @Test
  @DisplayName("sets values from both .env.local and .env. "
      + "but none is used as value existed in processEnv.")
  void existingProcessEnvValueWins() {
    Dotenv.processEnv().put("BASIC", "existing");

    ConfigResult env = quietly(() ->
        Dotenv.config(new ConfigOptions().path(List.of("tests/.env.local", "tests/.env"))));

    assertAll(
        // does not override processEnv
        () -> assertEquals("local_basic", env.parsed().get("BASIC")),
        () -> assertEquals("existing", Dotenv.processEnv().get("BASIC")));
  }

  @Test
  @DisplayName("takes URL for path option")
  void takesUrlForPathOption() {
    Path envPath = Paths.get("tests/.env").toAbsolutePath();
    URI fileUrl = URI.create("file://" + envPath);

    ConfigResult env = quietly(() -> Dotenv.config(new ConfigOptions().path(fileUrl)));

    assertAll(
        () -> assertEquals("basic", env.parsed().get("BASIC")),
        () -> assertEquals("basic", Dotenv.processEnv().get("BASIC")));
  }

  @Test
  @DisplayName("takes option for path along with home directory char ~")
  @SuppressWarnings("try") // the console capture is only here to silence output
  void takesPathWithHomeDirectoryChar() {
    String mockedHomedir = "/Users/dummy";

    try (MockedStatic<NodeFs> readFileSyncStub = mockStatic(NodeFs.class, CALLS_REAL_METHODS);
        MockedStatic<NodeOs> homedirStub = mockStatic(NodeOs.class, CALLS_REAL_METHODS);
        ConsoleCapture console = new ConsoleCapture()) {
      readFileSyncStub.when(() -> NodeFs.readFileSync(any(), any())).thenReturn("test=foo");
      homedirStub.when(NodeOs::homedir).thenReturn(mockedHomedir);
      readFileSyncStub.clearInvocations();
      homedirStub.clearInvocations();

      Dotenv.config(new ConfigOptions().path("~/.env"));

      ArgumentCaptor<Object> pathArg = ArgumentCaptor.forClass(Object.class);
      readFileSyncStub.verify(() -> NodeFs.readFileSync(pathArg.capture(), any()));
      assertEquals(NodePath.join(mockedHomedir, ".env"), pathArg.getValue());
      homedirStub.verify(NodeOs::homedir);
    }
  }

  @Test
  @DisplayName("takes option for encoding")
  @SuppressWarnings("try") // the console capture is only here to silence output
  void takesOptionForEncoding() {
    String testEncoding = "latin1";

    try (MockedStatic<NodeFs> readFileSyncStub = mockStatic(NodeFs.class, CALLS_REAL_METHODS);
        ConsoleCapture console = new ConsoleCapture()) {
      readFileSyncStub.when(() -> NodeFs.readFileSync(any(), any())).thenReturn("test=foo");
      readFileSyncStub.clearInvocations();

      Dotenv.config(new ConfigOptions().encoding(testEncoding));

      ArgumentCaptor<String> encodingArg = ArgumentCaptor.forClass(String.class);
      readFileSyncStub.verify(() -> NodeFs.readFileSync(any(), encodingArg.capture()));
      assertEquals(testEncoding, encodingArg.getValue());
    }
  }

  @Test
  @DisplayName("takes option for debug")
  void takesOptionForDebug() {
    boolean logged;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.config(new ConfigOptions().debug(true));
      logged = console.loggedOut();
    }

    assertTrue(logged);
  }

  @Test
  @DisplayName("reads path with encoding, parsing output to processEnv")
  @SuppressWarnings("try") // the console capture is only here to silence output
  void readsPathWithEncodingParsingOutput() {
    try (MockedStatic<NodeFs> readFileSyncStub = mockStatic(NodeFs.class, CALLS_REAL_METHODS);
        MockedStatic<Dotenv> parseStub = mockStatic(Dotenv.class, CALLS_REAL_METHODS);
        ConsoleCapture console = new ConsoleCapture()) {
      readFileSyncStub.when(() -> NodeFs.readFileSync(any(), any())).thenReturn("BASIC=basic");
      parseStub.when(() -> Dotenv.parse(any(String.class), any()))
          .thenReturn(Map.of("BASIC", "basic"));
      readFileSyncStub.clearInvocations();

      ConfigResult res = Dotenv.config();

      assertEquals(Map.of("BASIC", "basic"), res.parsed());
      readFileSyncStub.verify(() -> NodeFs.readFileSync(any(), any()), times(1));
    }
  }

  @Test
  @DisplayName("does not write over keys already in processEnv")
  void doesNotWriteOverExistingKeys() {
    String existing = "bar";
    Dotenv.processEnv().put("BASIC", existing);

    ConfigResult env = quietly(() -> Dotenv.config(new ConfigOptions().path("tests/.env")));

    assertAll(
        () -> assertEquals("basic", env.parsed().get("BASIC")),
        () -> assertEquals(existing, Dotenv.processEnv().get("BASIC")));
  }

  @Test
  @DisplayName("does write over keys already in processEnv if override turned on")
  void writesOverExistingKeysWhenOverride() {
    Dotenv.processEnv().put("BASIC", "bar");

    ConfigResult env = quietly(() ->
        Dotenv.config(new ConfigOptions().path("tests/.env").override(true)));

    assertAll(
        () -> assertEquals("basic", env.parsed().get("BASIC")),
        () -> assertEquals("basic", Dotenv.processEnv().get("BASIC")));
  }

  @Test
  @DisplayName("does not write over keys already in processEnv if the key has a falsy value")
  void doesNotWriteOverFalsyExistingKeys() {
    String existing = "";
    Dotenv.processEnv().put("BASIC", existing);

    ConfigResult env = quietly(() -> Dotenv.config(new ConfigOptions().path("tests/.env")));

    assertAll(
        () -> assertEquals("basic", env.parsed().get("BASIC")),
        () -> assertEquals("", Dotenv.processEnv().get("BASIC")));
  }

  @Test
  @DisplayName("does write over keys already in processEnv if the key has a falsy value "
      + "but override is set to true")
  void writesOverFalsyExistingKeysWhenOverride() {
    Dotenv.processEnv().put("BASIC", "");

    ConfigResult env = quietly(() ->
        Dotenv.config(new ConfigOptions().path("tests/.env").override(true)));

    assertAll(
        () -> assertEquals("basic", env.parsed().get("BASIC")),
        () -> assertEquals("basic", Dotenv.processEnv().get("BASIC")));
  }

  @Test
  @DisplayName("can write to a different object rather than processEnv")
  void canWriteToADifferentObject() {
    Dotenv.processEnv().put("BASIC", "other"); // reset processEnv
    Map<String, String> myObject = new LinkedHashMap<>();

    ConfigResult env = quietly(() ->
        Dotenv.config(new ConfigOptions().path("tests/.env").processEnv(myObject)));

    assertAll(
        () -> assertEquals("basic", env.parsed().get("BASIC")),
        () -> assertEquals("other", Dotenv.processEnv().get("BASIC")),
        () -> assertEquals("basic", myObject.get("BASIC")));
  }

  @Test
  @DisplayName("configDotenv loads without going through the secure branch")
  void configDotenvLoadsDirectly() {
    Dotenv.processEnv().put("DOTENV_CONFIG_PATH", "tests/.env");
    Dotenv.processEnv().put("DOTENV_CONFIG_QUIET", "true");

    ConfigResult env = ConsoleCapture.silenced(Dotenv::configDotenv);

    assertAll(
        () -> assertEquals("basic", env.parsed().get("BASIC")),
        () -> assertEquals("basic", Dotenv.processEnv().get("BASIC")));
  }

  @Test
  @DisplayName("configDotenv ignores secure, which config would honour")
  void configDotenvIgnoresSecure() {
    // config() would divert to dotenvx here; configDotenv() is the un-diverted path
    Dotenv.processEnv().put("DOTENV_CONFIG_SECURE", "true");

    ConfigResult env = ConsoleCapture.silenced(
        () -> Dotenv.configDotenv(new ConfigOptions().path("tests/.env").quiet(true)));

    assertAll(
        () -> assertNull(env.error()),
        () -> assertEquals("basic", env.parsed().get("BASIC")));
  }

  @Test
  @DisplayName("returns parsed object")
  void returnsParsedObject() {
    ConfigResult env = quietly(() -> Dotenv.config(new ConfigOptions().path("tests/.env")));

    assertAll(
        () -> assertNull(env.error()),
        () -> assertEquals("basic", env.parsed().get("BASIC")));
  }

  @Test
  @DisplayName("returns any errors thrown from reading file or parsing")
  @SuppressWarnings("try") // the console capture is only here to silence output
  void returnsErrorsThrownWhileReading() {
    try (MockedStatic<NodeFs> readFileSyncStub = mockStatic(NodeFs.class, CALLS_REAL_METHODS);
        ConsoleCapture console = new ConsoleCapture()) {
      readFileSyncStub.when(() -> NodeFs.readFileSync(any(), any()))
          .thenThrow(new IllegalStateException("Error"));

      ConfigResult env = Dotenv.config();

      assertInstanceOf(RuntimeException.class, env.error());
    }
  }

  @Test
  @DisplayName("logs any errors thrown from reading file or parsing when in debug mode")
  void logsErrorsThrownWhileReadingWhenDebug() {
    boolean logged;
    ConfigResult env;
    try (MockedStatic<NodeFs> readFileSyncStub = mockStatic(NodeFs.class, CALLS_REAL_METHODS);
        ConsoleCapture console = new ConsoleCapture()) {
      readFileSyncStub.when(() -> NodeFs.readFileSync(any(), any()))
          .thenThrow(new IllegalStateException("Error"));

      env = Dotenv.config(new ConfigOptions().debug(true));
      logged = console.loggedOut();
    }

    ConfigResult result = env;
    assertAll(
        () -> assertTrue(logged),
        () -> assertInstanceOf(RuntimeException.class, result.error()));
  }

  @Test
  @DisplayName("logs any errors parsing when in debug and override mode")
  void logsErrorsParsingWhenDebugAndOverride() {
    boolean logged;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.config(new ConfigOptions().debug(true).override(true));
      logged = console.loggedOut();
    }

    assertTrue(logged);
  }

  @Test
  @DisplayName("deals with file:// path")
  void dealsWithFileUrlPath() {
    boolean loggedErr;
    ConfigResult env;
    try (ConsoleCapture console = new ConsoleCapture()) {
      env = Dotenv.config(new ConfigOptions().path("file:///tests/.env"));
      loggedErr = console.loggedErr();
    }

    ConfigResult result = env;
    assertAll(
        () -> assertNull(result.parsed().get("BASIC")),
        () -> assertNull(Dotenv.processEnv().get("BASIC")),
        () -> assertEquals("ENOENT: no such file or directory, open 'file:///tests/.env'",
            result.error().getMessage()),
        () -> assertTrue(loggedErr));
  }

  @Test
  @DisplayName("deals with file:// path and debug true")
  void dealsWithFileUrlPathAndDebug() {
    boolean logged;
    ConfigResult env;
    try (ConsoleCapture console = new ConsoleCapture()) {
      env = Dotenv.config(new ConfigOptions().path("file:///tests/.env").debug(true));
      logged = console.loggedOut();
    }

    ConfigResult result = env;
    assertAll(
        () -> assertNull(result.parsed().get("BASIC")),
        () -> assertNull(Dotenv.processEnv().get("BASIC")),
        () -> assertEquals("ENOENT: no such file or directory, open 'file:///tests/.env'",
            result.error().getMessage()),
        () -> assertTrue(logged));
  }

  @Test
  @DisplayName("path.relative fails somehow")
  void pathRelativeFailsSomehow() {
    boolean logged;
    ConfigResult env;
    try (MockedStatic<NodePath> pathRelativeStub = mockStatic(NodePath.class, CALLS_REAL_METHODS);
        ConsoleCapture console = new ConsoleCapture()) {
      pathRelativeStub.when(() -> NodePath.relative(any(), any()))
          .thenThrow(new IllegalStateException("fail"));

      env = Dotenv.config(new ConfigOptions().path("file:///tests/.env").debug(true));
      logged = console.loggedOut();
    }

    ConfigResult result = env;
    assertAll(
        () -> assertNull(result.parsed().get("BASIC")),
        () -> assertNull(Dotenv.processEnv().get("BASIC")),
        () -> assertEquals("fail", result.error().getMessage()),
        () -> assertTrue(logged));
  }

  @Test
  @DisplayName("displays the injected env message without tips")
  void displaysInjectedEnvMessage() {
    String firstErrLine;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.config(new ConfigOptions().path("tests/.env"));
      firstErrLine = console.firstErrLine();
    }

    assertTrue(firstErrLine != null
            && firstErrLine.matches("^◇ injected env \\(\\d+\\) from tests/\\.env$"),
        "expected the injected env message, got: " + firstErrLine);
  }

  @Test
  @DisplayName("logs when no path is set")
  void logsWhenNoPathIsSet() {
    boolean loggedErr;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.config();
      loggedErr = console.loggedErr();
    }

    assertTrue(loggedErr);
  }

  @Test
  @DisplayName("does log by default")
  void logsByDefault() {
    boolean loggedErr;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.config(new ConfigOptions().path("tests/.env"));
      loggedErr = console.loggedErr();
    }

    assertTrue(loggedErr);
  }

  @Test
  @DisplayName("does not log if quiet flag passed true")
  void doesNotLogIfQuietTrue() {
    boolean loggedErr;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.config(new ConfigOptions().path("tests/.env").quiet(true));
      loggedErr = console.loggedErr();
    }

    assertFalse(loggedErr);
  }

  @Test
  @DisplayName("does log if quiet flag false")
  void logsIfQuietFalse() {
    boolean loggedErr;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.config(new ConfigOptions().path("tests/.env").quiet(false));
      loggedErr = console.loggedErr();
    }

    assertTrue(loggedErr);
  }

  @Test
  @DisplayName("does log if quiet flag present and undefined/null")
  void logsIfQuietUnset() {
    boolean loggedErr;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.config(new ConfigOptions().path("tests/.env"));
      loggedErr = console.loggedErr();
    }

    assertTrue(loggedErr);
  }

  @Test
  @DisplayName("logs if debug set")
  void logsIfDebugSet() {
    boolean logged;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.config(new ConfigOptions().path("tests/.env").debug(true));
      logged = console.loggedOut();
    }

    assertTrue(logged);
  }

  @Test
  @DisplayName("config with secure errors when dotenvx is not installed")
  void secureErrorsWhenDotenvxMissing() {
    String firstErrLine;
    ConfigResult result;
    try (ConsoleCapture console = new ConsoleCapture()) {
      result = Dotenv.config(new ConfigOptions().secure(true).quiet(true));
      firstErrLine = console.firstErrLine();
    }

    ConfigResult configResult = result;
    String errLine = firstErrLine;
    assertAll(
        () -> assertEquals("SECURE_REQUIRES_DOTENVX",
            assertInstanceOf(DotenvException.class, configResult.error()).code()),
        () -> assertTrue(errLine != null && errLine.contains("secure requires dotenvx"),
            "expected the secure hint, got: " + errLine));
  }

  @Test
  @DisplayName("DOTENV_CONFIG_SECURE=true errors when dotenvx is not installed")
  void secureFromEnvErrorsWhenDotenvxMissing() {
    Dotenv.processEnv().put("DOTENV_CONFIG_SECURE", "true");

    ConfigResult result =
        ConsoleCapture.silenced(() -> Dotenv.config(new ConfigOptions().quiet(true)));

    assertEquals("SECURE_REQUIRES_DOTENVX",
        assertInstanceOf(DotenvException.class, result.error()).code());
  }

  @Test
  @DisplayName("config warns when encrypted values are present without secure")
  void warnsWhenEncryptedValuesPresent() throws IOException {
    Path dir = Files.createTempDirectory("dotenv-secure-");
    Path envPath = dir.resolve(".env");
    Files.writeString(envPath, "HELLO=\"encrypted:abc123\"\n", StandardCharsets.UTF_8);

    Map<String, String> processEnv = new LinkedHashMap<>();
    List<String> errLines;
    ConfigResult result;
    try (ConsoleCapture console = new ConsoleCapture()) {
      result = Dotenv.config(new ConfigOptions()
          .path(envPath.toString()).processEnv(processEnv));
      errLines = console.errLines();
    }

    try (var paths = Files.walk(dir)) {
      paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.delete(p);
        } catch (IOException ignored) {
          // best effort, the temp directory is disposable
        }
      });
    }

    ConfigResult configResult = result;
    assertAll(
        () -> assertEquals("encrypted:abc123", processEnv.get("HELLO")),
        () -> assertEquals("encrypted:abc123", configResult.parsed().get("HELLO")),
        () -> assertTrue(errLines.contains("┆ encrypted values detected — use: "
            + "Dotenv.config(new ConfigOptions().secure(true))"),
            "expected the encrypted-values warning, got: " + errLines));
  }

  /** Runs {@code action} with the console captured, the way the noisier tests stubbed it. */
  private static ConfigResult quietly(java.util.function.Supplier<ConfigResult> action) {
    return ConsoleCapture.silenced(action);
  }
}
