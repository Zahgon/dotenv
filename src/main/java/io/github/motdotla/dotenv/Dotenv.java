package io.github.motdotla.dotenv;

import io.github.motdotla.dotenv.node.NodeFs;
import io.github.motdotla.dotenv.node.NodeOs;
import io.github.motdotla.dotenv.node.NodePath;
import io.github.motdotla.dotenv.node.NodeProcess;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads environment variables from a {@code .env} file.
 *
 * <p>See <a href="https://dotenvx.com/docs">https://dotenvx.com/docs</a>.
 */
public final class Dotenv {

  private Dotenv() {
  }

  /**
   * The environment dotenv reads defaults from and writes loaded values into.
   *
   * <p>Seeded from {@link System#getenv()} and mutable, standing in for Node's
   * {@code process.env}. A JVM cannot modify its own environment, so values loaded here are
   * visible to this process through this map and are passed on to child processes started
   * by {@link Cli}, rather than appearing in {@code System.getenv()}.
   */
  private static final Map<String, String> PROCESS_ENV = new LinkedHashMap<>(System.getenv());

  /** The mutable environment described above. */
  public static Map<String, String> processEnv() {
    return PROCESS_ENV;
  }

  /**
   * Parses a string in the {@code .env} file format into a map.
   *
   * @param src contents to be parsed, for example {@code "DB_HOST=localhost"}
   * @return the keys and values found in {@code src}, for example {@code {DB_HOST=localhost}}
   */
  public static Map<String, String> parse(String src) {
    return parse(src, null);
  }

  /** Parses UTF-8 encoded bytes in the {@code .env} file format into a map. */
  public static Map<String, String> parse(byte[] src) {
    return parse(src, null);
  }

  /**
   * Parses a string in the {@code .env} file format into a map.
   *
   * @param src contents to be parsed, for example {@code "DB_HOST=localhost"}
   * @param options parse options, for example {@code new ParseOptions().fast(true)}
   */
  public static Map<String, String> parse(String src, ParseOptions options) {
    if (options != null && options.fast()) {
      return FastParser.parse(src);
    }
    return RegexParser.parse(src);
  }

  /** Parses UTF-8 encoded bytes in the {@code .env} file format into a map. */
  public static Map<String, String> parse(byte[] src, ParseOptions options) {
    return parse(new String(src, StandardCharsets.UTF_8), options);
  }

  /** Loads {@code .env} file contents into {@link #processEnv()}. */
  public static ConfigResult config() {
    return config(null);
  }

  /**
   * Loads {@code .env} file contents into {@link #processEnv()} by default.
   *
   * @param options additional options
   * @return the parsed keys, or the error that occurred
   */
  public static ConfigResult config(ConfigOptions options) {
    ConfigOptions resolved = ConfigOptions.resolve(options, PROCESS_ENV);

    if (Js.parseBoolean(resolved.secure())) {
      return Dotenvx.config(resolved);
    }

    return Dotenv.configDotenv(resolved);
  }

  /** Loads {@code .env} file contents into {@link #processEnv()}, without dotenvx decryption. */
  public static ConfigResult configDotenv() {
    return configDotenv(null);
  }

  /**
   * Loads {@code .env} file contents into {@link #processEnv()}, without dotenvx decryption.
   *
   * @param options additional options
   * @return the parsed keys, or the error that occurred
   */
  public static ConfigResult configDotenv(ConfigOptions options) {
    ConfigOptions opts = ConfigOptions.resolve(options, PROCESS_ENV);
    String encoding = "utf8";
    Map<String, String> processEnv = opts.processEnv() != null ? opts.processEnv() : PROCESS_ENV;
    boolean debug = Js.parseBoolean(opts.debug());
    boolean quiet = Js.parseBoolean(opts.quiet());

    if (opts.hasTruthyEncoding()) {
      encoding = opts.encoding();
    } else if (debug) {
      logDebug("no encoding is specified (UTF-8 is used by default)");
    }

    List<Object> optionPaths = optionPaths(opts);

    // Build the parsed data in a temporary map (because we need to return it). Once we have
    // the final parsed data, we combine it with processEnv.
    RuntimeException lastError = null;
    Map<String, String> parsedAll = new LinkedHashMap<>();
    ParseOptions parseOptions = new ParseOptions().fast(Js.parseBoolean(opts.fast()));
    for (Object path : optionPaths) {
      try {
        Map<String, String> parsed = Dotenv.parse(NodeFs.readFileSync(path, encoding), parseOptions);
        Dotenv.populate(parsedAll, parsed, opts.toPopulateOptions());
      } catch (RuntimeException e) {
        if (debug) {
          logDebug("failed to load " + path + " " + e.getMessage());
        }
        lastError = e;
      }
    }

    boolean encrypted = hasEncryptedValues(parsedAll);
    Map<String, String> populated = Dotenv.populate(processEnv, parsedAll, opts.toPopulateOptions());

    if (debug || !quiet) {
      int keysCount = populated.size();
      List<String> shortPaths = new ArrayList<>();
      for (Object filePath : optionPaths) {
        try {
          shortPaths.add(NodePath.relative(NodeProcess.cwd(), filePath));
        } catch (RuntimeException e) {
          if (debug) {
            logDebug("failed to load " + filePath + " " + e.getMessage());
          }
          lastError = e;
        }
      }

      log("injected env (" + keysCount + ") from " + String.join(",", shortPaths));
    }

    if (encrypted) {
      System.err.println("┆ encrypted values detected — use: "
          + "Dotenv.config(new ConfigOptions().secure(true))");
    }

    return new ConfigResult(parsedAll, lastError);
  }

  /** Loads {@code source} contents into {@code target}, like {@link #processEnv()}. */
  public static Map<String, String> populate(
      Map<String, String> processEnv, Map<String, String> parsed) {
    return populate(processEnv, parsed, null);
  }

  /**
   * Loads {@code parsed} into {@code processEnv}.
   *
   * @param processEnv the target map; in most cases {@link #processEnv()}, but it can be any
   *     map you own
   * @param parsed the source map
   * @param options additional options
   * @return the keys and values that were actually set
   * @throws DotenvException with code {@code OBJECT_REQUIRED} if {@code parsed} is null
   */
  public static Map<String, String> populate(
      Map<String, String> processEnv, Map<String, String> parsed, PopulateOptions options) {
    boolean debug = options != null && options.debug();
    boolean override = options != null && options.override();
    Map<String, String> populated = new LinkedHashMap<>();

    if (parsed == null) {
      throw new DotenvException(
          "OBJECT_REQUIRED: Please check the processEnv argument being passed to populate",
          "OBJECT_REQUIRED");
    }

    // Set processEnv
    for (Map.Entry<String, String> entry : new ArrayList<>(parsed.entrySet())) {
      String key = entry.getKey();
      String value = entry.getValue();

      if (processEnv.containsKey(key)) {
        if (override) {
          processEnv.put(key, value);
          populated.put(key, value);
        }

        if (debug) {
          if (override) {
            logDebug("\"" + key + "\" is already defined and WAS overwritten");
          } else {
            logDebug("\"" + key + "\" is already defined and was NOT overwritten");
          }
        }
      } else {
        processEnv.put(key, value);
        populated.put(key, value);
      }
    }

    return populated;
  }

  /** The files to read, defaulting to {@code .env} in the working directory. */
  static List<Object> optionPaths(ConfigOptions opts) {
    List<Object> optionPaths = new ArrayList<>();

    if (!opts.hasTruthyPath()) {
      optionPaths.add(NodePath.resolve(".env")); // default, look for .env
      return optionPaths;
    }

    Object pathValue = opts.pathValue();
    if (pathValue instanceof List<?> paths) {
      for (Object filepath : paths) {
        optionPaths.add(resolveHome(filepath));
      }
    } else {
      optionPaths.add(resolveHome(pathValue));
    }
    return optionPaths;
  }

  private static Object resolveHome(Object envPath) {
    if (envPath instanceof String path && !path.isEmpty() && path.charAt(0) == '~') {
      return NodePath.join(NodeOs.homedir(), path.substring(1));
    }
    return envPath;
  }

  private static boolean hasEncryptedValues(Map<String, String> parsed) {
    for (String value : parsed.values()) {
      if (value != null && value.startsWith("encrypted:")) {
        return true;
      }
    }
    return false;
  }

  static void logDebug(String message) {
    System.out.println("┆ " + message);
  }

  static void log(String message) {
    System.err.println("◇ " + message);
  }
}
