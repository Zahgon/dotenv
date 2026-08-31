package io.github.motdotla.dotenv;

import io.github.motdotla.dotenv.node.NodeFs;
import io.github.motdotla.dotenv.node.NodeOs;
import io.github.motdotla.dotenv.node.NodePath;
import io.github.motdotla.dotenv.node.NodeProcess;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code dotenv run} command: runs a command with environment variables from a
 * {@code .env} file.
 */
public final class Cli {

  private Cli() {
  }

  private static final String HELP = String.join("\n",
      "Usage: dotenv run [--help] [--quiet] [--debug] [--override] [--secure] [--fast] "
          + "[-f <path>] -- <command>",
      "",
      "Run a command with environment variables from a .env file.",
      "",
      "Options:",
      "  -f <path>   path to your .env file (default: .env)",
      "  --quiet     suppress the injected env message",
      "  --debug     enable debug logging",
      "  --override  override existing environment variables",
      "  --secure    decrypt via dotenvx (requires dotenvx)",
      "  --fast      use the faster character-scanner parser",
      "",
      "Environment variables (same as former preload):",
      "  DOTENV_CONFIG_PATH, DOTENV_CONFIG_ENCODING, DOTENV_CONFIG_QUIET,",
      "  DOTENV_CONFIG_DEBUG, DOTENV_CONFIG_OVERRIDE, DOTENV_CONFIG_SECURE,",
      "  DOTENV_CONFIG_FAST");

  static void printHelp() {
    System.out.println(HELP);
  }

  /**
   * Runs the CLI.
   *
   * @param argv the arguments, excluding the program name
   * @return the process exit code
   */
  public static int run(String[] argv) {
    String command = argv.length == 0 ? null : argv[0];

    if ("--help".equals(command) || "-h".equals(command)) {
      printHelp();
      return 0;
    }

    if (!"run".equals(command)) {
      printHelp();
      return 1;
    }

    RunArgs parsed = parseRunArgs(java.util.Arrays.copyOfRange(argv, 1, argv.length));
    if (parsed.help()) {
      printHelp();
      return 0;
    }

    if (parsed.error() != null) {
      System.err.println("dotenv: " + parsed.error());
      printHelp();
      return 1;
    }

    if (parsed.command().isEmpty()) {
      printHelp();
      return 1;
    }

    RunOptions options = resolveRunOptions(parsed);

    if (options.secure()) {
      return runSecure(options, parsed.command());
    }

    try {
      Loaded result = loadEnvFiles(options);
      if (!options.quiet()) {
        StringBuilder message =
            new StringBuilder("◇ injected env (" + result.injected().size() + ")");
        if (!result.loadedPaths().isEmpty()) {
          message.append(" from ").append(String.join(", ", result.loadedPaths()));
        }
        System.err.println(message);
      }
      if (result.encrypted()) {
        System.err.println("┆ encrypted values detected — use: dotenv run --secure -- <command>");
      }
    } catch (RuntimeException e) {
      System.err.println("dotenv: " + e.getMessage());
      return 1;
    }

    return spawn(parsed.command());
  }

  /** The arguments as written on the command line, before environment defaults are applied. */
  private record RunArgs(
      boolean help,
      String error,
      List<String> paths,
      boolean pathSet,
      Boolean quiet,
      Boolean debug,
      Boolean override,
      Boolean secure,
      Boolean fast,
      List<String> command) {

    static RunArgs showHelp() {
      return new RunArgs(true, null, List.of(), false, null, null, null, null, null, List.of());
    }

    static RunArgs invalid(String message) {
      return new RunArgs(false, message, List.of(), false, null, null, null, null, null, List.of());
    }
  }

  /** The arguments merged with the {@code DOTENV_CONFIG_*} environment defaults. */
  record RunOptions(
      String encoding,
      boolean quiet,
      boolean debug,
      boolean override,
      boolean secure,
      boolean fast,
      List<String> paths,
      boolean defaultPath) {
  }

  /** What {@link #loadEnvFiles} injected, and where it came from. */
  private record Loaded(
      Map<String, String> injected, List<String> loadedPaths, boolean encrypted) {
  }

  private static RunArgs parseRunArgs(String[] args) {
    List<String> paths = new ArrayList<>();
    boolean pathSet = false;
    Boolean quiet = null;
    Boolean debug = null;
    Boolean override = null;
    Boolean secure = null;
    Boolean fast = null;
    int commandIndex = -1;

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      if (arg.equals("--")) {
        commandIndex = i + 1;
        break;
      }

      if (arg.equals("--help") || arg.equals("-h")) {
        return RunArgs.showHelp();
      }

      switch (arg) {
        case "--quiet" -> quiet = true;
        case "--debug" -> debug = true;
        case "--override" -> override = true;
        case "--secure" -> secure = true;
        case "--fast" -> fast = true;
        case "-f" -> {
          String filepath = i + 1 < args.length ? args[i + 1] : null;
          if (filepath == null || filepath.isEmpty() || filepath.equals("--")) {
            return RunArgs.invalid("-f requires a path");
          }
          paths.add(filepath);
          pathSet = true;
          i++;
        }
        default -> {
          if (arg.startsWith("-f=")) {
            String filepath = arg.substring(3);
            if (filepath.isEmpty()) {
              return RunArgs.invalid("-f requires a path");
            }
            paths.add(filepath);
            pathSet = true;
          } else {
            return RunArgs.invalid("unknown option: " + arg);
          }
        }
      }
    }

    List<String> command = commandIndex == -1
        ? List.of()
        : List.of(java.util.Arrays.copyOfRange(args, commandIndex, args.length));

    return new RunArgs(false, null, paths, pathSet, quiet, debug, override, secure, fast, command);
  }

  private static String resolveHome(String envPath) {
    return !envPath.isEmpty() && envPath.charAt(0) == '~'
        ? NodePath.join(NodeOs.homedir(), envPath.substring(1))
        : envPath;
  }

  private static RunOptions resolveRunOptions(RunArgs parsed) {
    Map<String, String> env = Dotenv.processEnv();

    String encoding = env.get("DOTENV_CONFIG_ENCODING");
    if (encoding == null || encoding.isEmpty()) {
      encoding = "utf8";
    }
    boolean quiet = Js.parseBoolean(env.get("DOTENV_CONFIG_QUIET"));
    boolean debug = Js.parseBoolean(env.get("DOTENV_CONFIG_DEBUG"));
    boolean override = Js.parseBoolean(env.get("DOTENV_CONFIG_OVERRIDE"));
    boolean secure = Js.parseBoolean(env.get("DOTENV_CONFIG_SECURE"));
    boolean fast = Js.parseBoolean(env.get("DOTENV_CONFIG_FAST"));

    List<String> paths = List.of(".env");
    boolean defaultPath = true;

    String envPath = env.get("DOTENV_CONFIG_PATH");
    if (envPath != null) {
      paths = List.of(envPath);
      defaultPath = false;
    }

    if (parsed.pathSet()) {
      paths = parsed.paths();
      defaultPath = false;
    }
    if (parsed.quiet() != null) {
      quiet = parsed.quiet();
    }
    if (parsed.debug() != null) {
      debug = parsed.debug();
    }
    if (parsed.override() != null) {
      override = parsed.override();
    }
    if (parsed.secure() != null) {
      secure = parsed.secure();
    }
    if (parsed.fast() != null) {
      fast = parsed.fast();
    }

    return new RunOptions(encoding, quiet, debug, override, secure, fast, paths, defaultPath);
  }

  static List<String> buildDotenvxArgs(RunOptions options, List<String> command) {
    List<String> args = new ArrayList<>();
    args.add("run");

    for (String filepath : options.paths()) {
      args.add("-f");
      args.add(filepath);
    }
    if (options.quiet()) {
      args.add("--quiet");
    }
    if (options.debug()) {
      args.add("--debug");
    }
    if (options.override()) {
      args.add("--overload");
    }
    args.add("--");
    args.addAll(command);

    return args;
  }

  private static void printSecureMissingError() {
    System.err.println("dotenv: --secure requires dotenvx");
    Dotenvx.printInstallHint();
  }

  private static int runSecure(RunOptions options, List<String> command) {
    String executable = Dotenvx.locate();
    if (executable == null) {
      printSecureMissingError();
      return 1;
    }

    List<String> full = new ArrayList<>();
    full.add(executable);
    full.addAll(buildDotenvxArgs(options, command));
    return spawn(full);
  }

  private static boolean hasEncryptedValues(Map<String, String> parsed) {
    for (String value : parsed.values()) {
      if (value != null && value.startsWith("encrypted:")) {
        return true;
      }
    }
    return false;
  }

  private static Loaded loadEnvFiles(RunOptions options) {
    Map<String, String> parsedAll = new LinkedHashMap<>();
    List<String> loadedPaths = new ArrayList<>();
    PopulateOptions populateOptions =
        new PopulateOptions().override(options.override()).debug(options.debug());

    for (String filepath : options.paths()) {
      String resolvedPath = NodePath.resolve(resolveHome(filepath));
      try {
        Map<String, String> parsed = Dotenv.parse(
            NodeFs.readFileSync(resolvedPath, options.encoding()),
            new ParseOptions().fast(options.fast()));
        Dotenv.populate(parsedAll, parsed, populateOptions);
        loadedPaths.add(filepath);
      } catch (RuntimeException e) {
        if (options.debug()) {
          System.out.println("┆ failed to load " + filepath + " " + e.getMessage());
        }
        boolean missingDefault = options.defaultPath()
            && e instanceof DotenvException dotenvError
            && "ENOENT".equals(dotenvError.code());
        if (!missingDefault) {
          throw e;
        }
      }
    }

    boolean encrypted = hasEncryptedValues(parsedAll);
    Map<String, String> injected =
        Dotenv.populate(Dotenv.processEnv(), parsedAll, populateOptions);
    return new Loaded(injected, loadedPaths, encrypted);
  }

  /**
   * Runs {@code command} with the injected environment, wired to this process's streams.
   *
   * @return the child's exit code, or {@code 128 + signal} if it was killed by a signal
   */
  private static int spawn(List<String> command) {
    List<String> full = new ArrayList<>();
    if ("win32".equals(NodeOs.platform())) {
      full.add("cmd.exe");
      full.add("/c");
    }
    full.addAll(command);

    try {
      ProcessBuilder builder = new ProcessBuilder(full).inheritIO();
      builder.directory(new java.io.File(NodeProcess.cwd()));
      builder.environment().clear();
      builder.environment().putAll(Dotenv.processEnv());
      return builder.start().waitFor();
    } catch (IOException e) {
      System.err.println("dotenv: spawn " + command.get(0) + " ENOENT");
      return 1;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("dotenv: interrupted");
      return 1;
    }
  }
}
