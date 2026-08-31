package io.github.motdotla.dotenv;

/**
 * Loads {@code dotenv/config} and prints a value, for {@link ConfigImportTest} to run in a
 * fresh JVM.
 *
 * <p>The counterpart of the original's
 * {@code node --eval "import 'dotenv/config'; console.log(process.env.BASIC)"}.
 */
public final class ConfigImportProbe {

  private ConfigImportProbe() {
  }

  public static void main(String[] args) {
    Config.load();
    System.out.println(Dotenv.processEnv().get("BASIC"));
  }
}
