package io.github.motdotla.dotenv;

import io.github.motdotla.dotenv.node.NodeOs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Decryption via <a href="https://dotenvx.com">dotenvx</a>, used by the {@code secure} option.
 *
 * <p>The original required {@code @dotenvx/dotenvx} into the same process. A JVM has no way
 * to do that, so this locates the {@code dotenvx} executable on {@code PATH} and reads the
 * decrypted values back from it. When dotenvx is absent the observable result is unchanged:
 * the same three lines on stderr and a {@code SECURE_REQUIRES_DOTENVX} error.
 */
final class Dotenvx {

  private Dotenvx() {
  }

  static ConfigResult config(ConfigOptions options) {
    String executable = locate();
    if (executable == null) {
      System.err.println("dotenv: secure requires dotenvx");
      printInstallHint();
      return new ConfigResult(null, secureRequiresDotenvxError());
    }

    List<Object> paths = options.hasTruthyPath() ? Dotenv.optionPaths(options) : List.of();

    List<String> command = new ArrayList<>();
    command.add(executable);
    command.add("get");
    command.add("--format");
    command.add("json");
    for (Object path : paths) {
      command.add("-f");
      command.add(String.valueOf(path));
    }

    String json;
    try {
      ProcessBuilder builder = new ProcessBuilder(command);
      builder.environment().clear();
      builder.environment().putAll(Dotenv.processEnv());
      // dotenvx writes its own diagnostics to stderr; let them through untouched
      builder.redirectError(ProcessBuilder.Redirect.INHERIT);
      Process process = builder.start();
      json = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      int status = process.waitFor();
      if (status != 0) {
        return new ConfigResult(null, new DotenvException(
            "SECURE_DOTENVX_FAILED: dotenvx exited with code " + status,
            "SECURE_DOTENVX_FAILED"));
      }
    } catch (IOException e) {
      return new ConfigResult(null, new DotenvException(
          "SECURE_DOTENVX_FAILED: " + e.getMessage(), "SECURE_DOTENVX_FAILED"));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new ConfigResult(null, new DotenvException(
          "SECURE_DOTENVX_FAILED: interrupted", "SECURE_DOTENVX_FAILED"));
    }

    Map<String, String> parsed;
    try {
      parsed = Json.parseObject(json);
    } catch (IllegalArgumentException e) {
      return new ConfigResult(null, new DotenvException(
          "SECURE_DOTENVX_FAILED: " + e.getMessage(), "SECURE_DOTENVX_FAILED"));
    }

    Map<String, String> processEnv =
        options.processEnv() != null ? options.processEnv() : Dotenv.processEnv();
    Map<String, String> populated =
        Dotenv.populate(processEnv, parsed, options.toPopulateOptions());

    if (!Js.parseBoolean(options.quiet())) {
      Dotenv.log("injected env (" + populated.size() + ") from dotenvx");
    }

    return new ConfigResult(parsed, null);
  }

  /** The path to the {@code dotenvx} executable, or {@code null} when it is not installed. */
  static String locate() {
    String which = "win32".equals(NodeOs.platform()) ? "where" : "which";
    try {
      Process process = new ProcessBuilder(which, "dotenvx")
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start();
      String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      if (process.waitFor() != 0) {
        return null;
      }
      for (String line : stdout.split("\\r?\\n")) {
        if (!line.isBlank()) {
          return line;
        }
      }
    } catch (IOException e) {
      return null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return null;
  }

  static void printInstallHint() {
    System.err.println("  npm i @dotenvx/dotenvx");
    System.err.println("  # or: curl -sfS https://dotenvx.sh | sh");
  }

  static DotenvException secureRequiresDotenvxError() {
    return new DotenvException(
        "SECURE_REQUIRES_DOTENVX: config(new ConfigOptions().secure(true)) requires dotenvx. "
            + "Install with: npm i @dotenvx/dotenvx",
        "SECURE_REQUIRES_DOTENVX");
  }
}
