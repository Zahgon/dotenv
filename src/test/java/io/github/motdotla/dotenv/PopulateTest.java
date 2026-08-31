package io.github.motdotla.dotenv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import io.github.motdotla.dotenv.node.NodeFs;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Ported from {@code tests/test-populate.js}. */
class PopulateTest {

  private static final Map<String, String> MOCK_PARSE_RESPONSE = Map.of("test", "foo");

  private MockedStatic<NodeFs> readFileSyncStub;
  private MockedStatic<Dotenv> parseStub;

  @BeforeEach
  void setUp() {
    readFileSyncStub = mockStatic(NodeFs.class, CALLS_REAL_METHODS);
    readFileSyncStub.when(() -> NodeFs.readFileSync(any(), any())).thenReturn("test=foo");
    parseStub = mockStatic(Dotenv.class, CALLS_REAL_METHODS);
    parseStub.when(() -> Dotenv.parse(any(String.class), any())).thenReturn(MOCK_PARSE_RESPONSE);
  }

  @AfterEach
  void tearDown() {
    parseStub.close();
    readFileSyncStub.close();
    Dotenv.processEnv().remove("test");
  }

  @Test
  @DisplayName("takes processEnv and check if all keys applied to processEnv")
  void appliesAllKeysToProcessEnv() {
    Map<String, String> parsed = new LinkedHashMap<>();
    parsed.put("test", "1");
    parsed.put("home", "2");
    Map<String, String> processEnv = new LinkedHashMap<>();

    Dotenv.populate(processEnv, parsed);

    assertEquals(parsed, processEnv);
  }

  @Test
  @DisplayName("does not write over keys already in processEnv")
  void doesNotWriteOverExistingKeys() {
    String existing = "bar";
    Map<String, String> parsed = Map.of("test", "test");
    Dotenv.processEnv().put("test", existing);

    // 'test' returned as value in setUp. should keep this 'bar'
    Dotenv.populate(Dotenv.processEnv(), parsed);

    assertEquals(existing, Dotenv.processEnv().get("test"));
  }

  @Test
  @DisplayName("does write over keys already in processEnv if override turned on")
  void writesOverExistingKeysWhenOverride() {
    Map<String, String> parsed = Map.of("test", "test");
    Dotenv.processEnv().put("test", "bar");

    // 'test' returned as value in setUp. should change this 'bar' to 'test'
    Dotenv.populate(Dotenv.processEnv(), parsed, new PopulateOptions().override(true));

    assertEquals(parsed.get("test"), Dotenv.processEnv().get("test"));
  }

  @Test
  @DisplayName("logs any errors populating when in debug mode but override turned off")
  void logsWhenDebugAndOverrideOff() {
    Map<String, String> parsed = Map.of("test", "false");
    Dotenv.processEnv().put("test", "true");

    boolean logged;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.populate(Dotenv.processEnv(), parsed, new PopulateOptions().debug(true));
      logged = console.loggedOut();
    }

    assertNotEquals(parsed.get("test"), Dotenv.processEnv().get("test"));
    assertTrue(logged);
  }

  @Test
  @DisplayName("logs populating when debug mode and override turned on")
  void logsWhenDebugAndOverrideOn() {
    Map<String, String> parsed = Map.of("test", "false");
    Dotenv.processEnv().put("test", "true");

    boolean logged;
    try (ConsoleCapture console = new ConsoleCapture()) {
      Dotenv.populate(Dotenv.processEnv(), parsed,
          new PopulateOptions().debug(true).override(true));
      logged = console.loggedOut();
    }

    assertTrue(logged);
  }

  @Test
  @DisplayName("returns any errors thrown on passing not json type")
  void throwsOnNonObjectParsed() {
    DotenvException e = assertThrows(DotenvException.class,
        () -> Dotenv.populate(Dotenv.processEnv(), null));

    assertEquals(
        "OBJECT_REQUIRED: Please check the processEnv argument being passed to populate",
        e.getMessage());
  }
}
