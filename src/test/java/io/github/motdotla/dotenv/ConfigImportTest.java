package io.github.motdotla.dotenv;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Ported from {@code tests/test-config-import.js}. */
class ConfigImportTest {

  @Test
  @DisplayName("loading 'dotenv/config' loads env before application code")
  void loadsEnvBeforeApplicationCode() throws Exception {
    String java = Paths.get(System.getProperty("java.home"), "bin", "java").toString();

    ProcessBuilder builder = new ProcessBuilder(
        java,
        "-cp", System.getProperty("java.class.path"),
        "-Dstdout.encoding=UTF-8",
        "-Dstderr.encoding=UTF-8",
        ConfigImportProbe.class.getName());
    builder.directory(new File(System.getProperty("user.dir")));
    builder.environment().put("DOTENV_CONFIG_PATH", "tests/.env");
    builder.environment().put("DOTENV_CONFIG_QUIET", "true");

    Process process = builder.start();
    String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    int status = process.waitFor();

    assertAll(
        () -> assertEquals(0, status),
        () -> assertEquals("basic\n", stdout),
        () -> assertEquals("", stderr));
  }
}
