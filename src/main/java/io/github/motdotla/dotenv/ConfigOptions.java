package io.github.motdotla.dotenv;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Options for {@link Dotenv#config(ConfigOptions)} and
 * {@link Dotenv#configDotenv(ConfigOptions)}.
 *
 * <p>Every option can also be supplied through a {@code DOTENV_CONFIG_*} environment
 * variable. Options set here win over those defaults — and, as in the original, setting an
 * option explicitly wins even when it is set to {@code null}, which is why each field
 * tracks whether it was assigned at all rather than inferring it from the value.
 */
public final class ConfigOptions {

  private String encoding;
  private boolean encodingSet;

  private Object path;
  private boolean pathSet;

  private Boolean quiet;
  private boolean quietSet;

  private Boolean debug;
  private boolean debugSet;

  private Boolean override;
  private boolean overrideSet;

  private Boolean secure;
  private boolean secureSet;

  private Boolean fast;
  private boolean fastSet;

  private Map<String, String> processEnv;
  private boolean processEnvSet;

  /**
   * The encoding of the file containing your environment variables.
   *
   * <p>Default: {@code utf8}. Example: {@code new ConfigOptions().encoding("latin1")}
   */
  public ConfigOptions encoding(String encoding) {
    this.encoding = encoding;
    this.encodingSet = true;
    return this;
  }

  /**
   * A custom path, if your file containing environment variables is located elsewhere.
   *
   * <p>Default: {@code .env} resolved against the working directory. A leading {@code ~} is
   * expanded to the home directory.
   */
  public ConfigOptions path(String path) {
    this.path = path;
    this.pathSet = true;
    return this;
  }

  /** Several paths, loaded in order. The first file to define a key wins. */
  public ConfigOptions path(List<String> paths) {
    this.path = paths;
    this.pathSet = true;
    return this;
  }

  /** A {@code file:} URI pointing at your environment file. */
  public ConfigOptions path(URI path) {
    this.path = path;
    this.pathSet = true;
    return this;
  }

  /**
   * Suppress all output except errors.
   *
   * <p>Default: {@code false}.
   */
  public ConfigOptions quiet(boolean quiet) {
    this.quiet = quiet;
    this.quietSet = true;
    return this;
  }

  /**
   * Turn on logging to help debug why certain keys or values are not being set as you expect.
   *
   * <p>Default: {@code false}.
   */
  public ConfigOptions debug(boolean debug) {
    this.debug = debug;
    this.debugSet = true;
    return this;
  }

  /**
   * Override any environment variables that have already been set on your machine with
   * values from your {@code .env} file.
   *
   * <p>Default: {@code false}.
   */
  public ConfigOptions override(boolean override) {
    this.override = override;
    this.overrideSet = true;
    return this;
  }

  /**
   * Decrypt via dotenvx. Requires the {@code dotenvx} CLI on your {@code PATH}.
   *
   * <p>Default: {@code false}.
   */
  public ConfigOptions secure(boolean secure) {
    this.secure = secure;
    this.secureSet = true;
    return this;
  }

  /**
   * Use the faster character-scanner parser.
   *
   * <p>Default: {@code false}.
   */
  public ConfigOptions fast(boolean fast) {
    this.fast = fast;
    this.fastSet = true;
    return this;
  }

  /**
   * The map to write your secrets to.
   *
   * <p>Default: {@link Dotenv#processEnv()}.
   */
  public ConfigOptions processEnv(Map<String, String> processEnv) {
    this.processEnv = processEnv;
    this.processEnvSet = true;
    return this;
  }

  String encoding() {
    return encoding;
  }

  Object pathValue() {
    return path;
  }

  Boolean quiet() {
    return quiet;
  }

  Boolean debug() {
    return debug;
  }

  Boolean override() {
    return override;
  }

  Boolean secure() {
    return secure;
  }

  Boolean fast() {
    return fast;
  }

  Map<String, String> processEnv() {
    return processEnv;
  }

  /** Whether a path was supplied and is truthy, the way the original tested {@code options.path}. */
  boolean hasTruthyPath() {
    if (!pathSet || path == null) {
      return false;
    }
    return !(path instanceof String string) || !string.isEmpty();
  }

  /** Whether an encoding was supplied and is truthy. */
  boolean hasTruthyEncoding() {
    return encodingSet && encoding != null && !encoding.isEmpty();
  }

  PopulateOptions toPopulateOptions() {
    return new PopulateOptions()
        .debug(debug() != null && debug())
        .override(override() != null && override());
  }

  /**
   * Layers these options over the {@code DOTENV_CONFIG_*} defaults read from {@code env}.
   *
   * <p>Only options that were explicitly assigned are layered on, so an unset option falls
   * through to its environment default while an option assigned {@code null} overrides one.
   */
  static ConfigOptions resolve(ConfigOptions options, Map<String, String> env) {
    ConfigOptions resolved = new ConfigOptions();

    String envEncoding = env.get("DOTENV_CONFIG_ENCODING");
    if (envEncoding != null) {
      resolved.encoding(envEncoding);
    }
    String envPath = env.get("DOTENV_CONFIG_PATH");
    if (envPath != null) {
      resolved.path(envPath);
    }
    String envQuiet = env.get("DOTENV_CONFIG_QUIET");
    if (envQuiet != null) {
      resolved.quiet(Js.parseBoolean(envQuiet));
    }
    String envDebug = env.get("DOTENV_CONFIG_DEBUG");
    if (envDebug != null) {
      resolved.debug(Js.parseBoolean(envDebug));
    }
    String envOverride = env.get("DOTENV_CONFIG_OVERRIDE");
    if (envOverride != null) {
      resolved.override(Js.parseBoolean(envOverride));
    }
    String envSecure = env.get("DOTENV_CONFIG_SECURE");
    if (envSecure != null) {
      resolved.secure(Js.parseBoolean(envSecure));
    }
    String envFast = env.get("DOTENV_CONFIG_FAST");
    if (envFast != null) {
      resolved.fast(Js.parseBoolean(envFast));
    }

    if (options == null) {
      return resolved;
    }

    if (options.encodingSet) {
      resolved.encoding = options.encoding;
      resolved.encodingSet = true;
    }
    if (options.pathSet) {
      resolved.path = options.path;
      resolved.pathSet = true;
    }
    if (options.quietSet) {
      resolved.quiet = options.quiet;
      resolved.quietSet = true;
    }
    if (options.debugSet) {
      resolved.debug = options.debug;
      resolved.debugSet = true;
    }
    if (options.overrideSet) {
      resolved.override = options.override;
      resolved.overrideSet = true;
    }
    if (options.secureSet) {
      resolved.secure = options.secure;
      resolved.secureSet = true;
    }
    if (options.fastSet) {
      resolved.fast = options.fast;
      resolved.fastSet = true;
    }
    if (options.processEnvSet) {
      resolved.processEnv = options.processEnv;
      resolved.processEnvSet = true;
    }

    return resolved;
  }
}
