package io.github.motdotla.dotenv;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** The {@code dotenv} executable's entry point. */
public final class Main {

  private Main() {
  }

  public static void main(String[] args) {
    // The injected-env and debug lines are drawn with ◇ and ┆, so pin the console to UTF-8
    // rather than inheriting a platform encoding that cannot represent them.
    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true,
        StandardCharsets.UTF_8));
    System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true,
        StandardCharsets.UTF_8));

    System.exit(Cli.run(args));
  }
}
