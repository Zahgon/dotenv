package io.github.motdotla.dotenv;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import io.github.motdotla.dotenv.node.NodeOs;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers {@code dotenv run}, whose argument parsing and help rendering the port implements
 * itself.
 *
 * <p>The original had no CLI tests, so its help text and exit codes were only ever verified
 * by hand. They are part of the CLI's observable surface, so they are pinned here.
 */
class CliTest {

  private Map<String, String> savedEnv;

  @BeforeEach
  void saveEnv() {
    savedEnv = new LinkedHashMap<>(Dotenv.processEnv());
    // start from a clean slate so the fixture keys are not inherited from an earlier class
    List.of("BASIC", "LOCAL", "SINGLE_QUOTES").forEach(Dotenv.processEnv()::remove);
  }

  @AfterEach
  void restoreEnv() {
    Dotenv.processEnv().clear();
    Dotenv.processEnv().putAll(savedEnv);
  }

  /** Runs the CLI with the console captured. */
  private record Result(int exitCode, List<String> out, List<String> err) {
  }

  private static Result runCli(String... argv) {
    try (ConsoleCapture console = new ConsoleCapture()) {
      int exitCode = Cli.run(argv);
      return new Result(exitCode, console.outLines(), console.errLines());
    }
  }

  private static final String USAGE_LINE =
      "Usage: dotenv run [--help] [--quiet] [--debug] [--override] [--secure] [--fast] "
          + "[-f <path>] -- <command>";

  @Test
  @DisplayName("--help prints usage to stdout and exits 0")
  void helpPrintsUsage() {
    Result result = runCli("--help");

    assertAll(
        () -> assertEquals(0, result.exitCode()),
        () -> assertEquals(USAGE_LINE, result.out().get(0)),
        () -> assertTrue(result.out().contains("  --quiet     suppress the injected env message")),
        () -> assertTrue(result.out().contains("  DOTENV_CONFIG_FAST")),
        () -> assertTrue(result.err().isEmpty()));
  }

  @Test
  @DisplayName("-h prints usage to stdout and exits 0")
  void shortHelpPrintsUsage() {
    Result result = runCli("-h");

    assertAll(
        () -> assertEquals(0, result.exitCode()),
        () -> assertEquals(USAGE_LINE, result.out().get(0)));
  }

  @Test
  @DisplayName("run --help prints usage and exits 0")
  void runHelpPrintsUsage() {
    Result result = runCli("run", "--help");

    assertAll(
        () -> assertEquals(0, result.exitCode()),
        () -> assertEquals(USAGE_LINE, result.out().get(0)));
  }

  @Test
  @DisplayName("no arguments prints usage and exits 1")
  void noArgumentsExitsOne() {
    Result result = runCli();

    assertAll(
        () -> assertEquals(1, result.exitCode()),
        () -> assertEquals(USAGE_LINE, result.out().get(0)));
  }

  @Test
  @DisplayName("an unknown command prints usage and exits 1")
  void unknownCommandExitsOne() {
    Result result = runCli("nope");

    assertAll(
        () -> assertEquals(1, result.exitCode()),
        () -> assertEquals(USAGE_LINE, result.out().get(0)));
  }

  @Test
  @DisplayName("run without a command prints usage and exits 1")
  void runWithoutCommandExitsOne() {
    Result result = runCli("run");

    assertAll(
        () -> assertEquals(1, result.exitCode()),
        () -> assertEquals(USAGE_LINE, result.out().get(0)));
  }

  @Test
  @DisplayName("run with an empty command after -- prints usage and exits 1")
  void runWithEmptyCommandExitsOne() {
    Result result = runCli("run", "--");

    assertAll(
        () -> assertEquals(1, result.exitCode()),
        () -> assertEquals(USAGE_LINE, result.out().get(0)));
  }

  @Test
  @DisplayName("an unknown option is reported and exits 1")
  void unknownOptionExitsOne() {
    Result result = runCli("run", "--bogus", "--", "echo");

    assertAll(
        () -> assertEquals(1, result.exitCode()),
        () -> assertEquals("dotenv: unknown option: --bogus", result.err().get(0)),
        () -> assertEquals(USAGE_LINE, result.out().get(0)));
  }

  @Test
  @DisplayName("-f without a path is reported and exits 1")
  void dashFWithoutPathExitsOne() {
    Result result = runCli("run", "-f");

    assertAll(
        () -> assertEquals(1, result.exitCode()),
        () -> assertEquals("dotenv: -f requires a path", result.err().get(0)));
  }

  @Test
  @DisplayName("-f followed by -- is reported and exits 1")
  void dashFFollowedByDashDashExitsOne() {
    Result result = runCli("run", "-f", "--", "echo");

    assertAll(
        () -> assertEquals(1, result.exitCode()),
        () -> assertEquals("dotenv: -f requires a path", result.err().get(0)));
  }

  @Test
  @DisplayName("-f= without a path is reported and exits 1")
  void dashFEqualsWithoutPathExitsOne() {
    Result result = runCli("run", "-f=", "--", "echo");

    assertAll(
        () -> assertEquals(1, result.exitCode()),
        () -> assertEquals("dotenv: -f requires a path", result.err().get(0)));
  }

  @Test
  @DisplayName("a missing explicit -f file is reported and exits 1")
  void missingExplicitFileExitsOne() {
    Result result = runCli("run", "-f", "tests/.env.does-not-exist", "--", "true");

    assertAll(
        () -> assertEquals(1, result.exitCode()),
        () -> assertTrue(result.err().get(0).startsWith("dotenv: ENOENT: no such file"),
            "got: " + result.err()));
  }

  @Test
  @DisplayName("run injects the env file and reports what it injected")
  void injectsEnvAndReports() {
    assumeFalse("win32".equals(NodeOs.platform()));

    Result result = runCli("run", "-f", "tests/.env", "--", "true");

    assertAll(
        () -> assertEquals(0, result.exitCode()),
        () -> assertEquals("basic", Dotenv.processEnv().get("BASIC")),
        () -> assertTrue(result.err().get(0)
            .matches("^◇ injected env \\(\\d+\\) from tests/\\.env$"), "got: " + result.err()));
  }

  @Test
  @DisplayName("--quiet suppresses the injected env message")
  void quietSuppressesMessage() {
    assumeFalse("win32".equals(NodeOs.platform()));

    Result result = runCli("run", "--quiet", "-f", "tests/.env", "--", "true");

    assertAll(
        () -> assertEquals(0, result.exitCode()),
        () -> assertTrue(result.err().isEmpty(), "got: " + result.err()));
  }

  @Test
  @DisplayName("-f= accepts an inline path")
  void dashFEqualsAcceptsInlinePath() {
    assumeFalse("win32".equals(NodeOs.platform()));

    Result result = runCli("run", "--quiet", "-f=tests/.env.local", "--", "true");

    assertAll(
        () -> assertEquals(0, result.exitCode()),
        () -> assertEquals("local_basic", Dotenv.processEnv().get("BASIC")));
  }

  @Test
  @DisplayName("several -f files are loaded in order, the first winning")
  void severalFilesFirstWins() {
    assumeFalse("win32".equals(NodeOs.platform()));

    Result result =
        runCli("run", "--quiet", "-f", "tests/.env.local", "-f", "tests/.env", "--", "true");

    assertAll(
        () -> assertEquals(0, result.exitCode()),
        () -> assertEquals("local_basic", Dotenv.processEnv().get("BASIC")),
        () -> assertEquals("single_quotes", Dotenv.processEnv().get("SINGLE_QUOTES")));
  }

  @Test
  @DisplayName("--override replaces a value already in the environment")
  void overrideReplacesExistingValue() {
    assumeFalse("win32".equals(NodeOs.platform()));
    Dotenv.processEnv().put("BASIC", "existing");

    runCli("run", "--quiet", "--override", "-f", "tests/.env", "--", "true");

    assertEquals("basic", Dotenv.processEnv().get("BASIC"));
  }

  @Test
  @DisplayName("without --override an existing value is kept")
  void withoutOverrideExistingValueIsKept() {
    assumeFalse("win32".equals(NodeOs.platform()));
    Dotenv.processEnv().put("BASIC", "existing");

    runCli("run", "--quiet", "-f", "tests/.env", "--", "true");

    assertEquals("existing", Dotenv.processEnv().get("BASIC"));
  }

  @Test
  @DisplayName("the child's exit code is propagated")
  void propagatesChildExitCode() {
    assumeFalse("win32".equals(NodeOs.platform()));

    Result result = runCli("run", "--quiet", "-f", "tests/.env", "--", "sh", "-c", "exit 7");

    assertEquals(7, result.exitCode());
  }

  @Test
  @DisplayName("the child sees the injected environment")
  void childSeesInjectedEnvironment() {
    assumeFalse("win32".equals(NodeOs.platform()));

    Result result = runCli("run", "--quiet", "-f", "tests/.env", "--",
        "sh", "-c", "test \"$SINGLE_QUOTES\" = single_quotes");

    assertEquals(0, result.exitCode());
  }

  @Test
  @DisplayName("a command that cannot be run is reported and exits 1")
  void unrunnableCommandExitsOne() {
    Result result = runCli("run", "--quiet", "-f", "tests/.env", "--", "no-such-command-xyz");

    assertAll(
        () -> assertEquals(1, result.exitCode()),
        () -> assertEquals("dotenv: spawn no-such-command-xyz ENOENT", result.err().get(0)));
  }

  @Test
  @DisplayName("the dotenvx argument vector is built from the resolved options")
  void buildsDotenvxArguments() {
    // only reachable with dotenvx installed, so it is pinned directly rather than left
    // to be discovered by whoever first runs --secure on a machine that has it
    Cli.RunOptions options = new Cli.RunOptions(
        "utf8", true, true, true, true, false, List.of(".env.local", ".env"), false);

    assertEquals(
        List.of("run", "-f", ".env.local", "-f", ".env",
            "--quiet", "--debug", "--overload", "--", "node", "index.js"),
        Cli.buildDotenvxArgs(options, List.of("node", "index.js")));
  }

  @Test
  @DisplayName("the dotenvx argument vector omits flags that are off")
  void buildsMinimalDotenvxArguments() {
    Cli.RunOptions options = new Cli.RunOptions(
        "utf8", false, false, false, true, false, List.of(".env"), true);

    assertEquals(
        List.of("run", "-f", ".env", "--", "echo", "hi"),
        Cli.buildDotenvxArgs(options, List.of("echo", "hi")));
  }

  @Test
  @DisplayName("--secure without dotenvx installed is reported and exits 1")
  void secureWithoutDotenvxExitsOne() {
    Result result = runCli("run", "--secure", "-f", "tests/.env", "--", "true");

    assertAll(
        () -> assertEquals(1, result.exitCode()),
        () -> assertEquals("dotenv: --secure requires dotenvx", result.err().get(0)),
        () -> assertEquals("  npm i @dotenvx/dotenvx", result.err().get(1)));
  }

  @Test
  @DisplayName("a missing default .env is not an error")
  void missingDefaultEnvIsNotAnError() {
    assumeFalse("win32".equals(NodeOs.platform()));

    Result result = runCli("run", "--quiet", "--", "true");

    assertEquals(0, result.exitCode());
  }

  @Test
  @DisplayName("DOTENV_CONFIG_PATH supplies the default file")
  void configPathEnvSuppliesDefaultFile() {
    assumeFalse("win32".equals(NodeOs.platform()));
    Dotenv.processEnv().put("DOTENV_CONFIG_PATH", "tests/.env.local");
    Dotenv.processEnv().put("DOTENV_CONFIG_QUIET", "true");

    Result result = runCli("run", "--", "true");

    assertAll(
        () -> assertEquals(0, result.exitCode()),
        () -> assertEquals("local_basic", Dotenv.processEnv().get("BASIC")),
        () -> assertTrue(result.err().isEmpty(), "got: " + result.err()));
  }

  @Test
  @DisplayName("--debug logs why a file could not be loaded")
  void debugLogsLoadFailure() {
    Result result = runCli("run", "--debug", "-f", "tests/.env.nope", "--", "true");

    assertAll(
        () -> assertEquals(1, result.exitCode()),
        () -> assertTrue(result.out().get(0).startsWith("┆ failed to load tests/.env.nope"),
            "got: " + result.out()));
  }
}
