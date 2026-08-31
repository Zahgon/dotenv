package io.github.motdotla.dotenv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link Config} in-process; {@link ConfigImportTest} covers it end to end in a fresh
 * JVM, which is where the load-before-application-code ordering actually matters.
 */
class ConfigLoadTest {

  @Test
  @DisplayName("Config.load() loads the file named by DOTENV_CONFIG_PATH, once")
  void loadsOnFirstTouch() {
    Dotenv.processEnv().remove("BASIC");
    Dotenv.processEnv().put("DOTENV_CONFIG_PATH", "tests/.env");
    Dotenv.processEnv().put("DOTENV_CONFIG_QUIET", "true");
    try {
      Config.load();
      assertEquals("basic", Dotenv.processEnv().get("BASIC"));

      // the static initializer has already run, so a second touch changes nothing
      Dotenv.processEnv().put("BASIC", "untouched");
      Config.load();
      assertEquals("untouched", Dotenv.processEnv().get("BASIC"));
    } finally {
      Dotenv.processEnv().remove("DOTENV_CONFIG_PATH");
      Dotenv.processEnv().remove("DOTENV_CONFIG_QUIET");
      Dotenv.processEnv().remove("BASIC");
    }
  }
}
