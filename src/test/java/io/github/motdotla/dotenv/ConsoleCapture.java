package io.github.motdotla.dotenv;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Captures what the code under test writes to the console.
 *
 * <p>Stands in for the {@code sinon.stub(console, 'log')} and {@code sinon.stub(console,
 * 'error')} the original tests used: it both silences the output and records it, and
 * restores the real streams when closed.
 */
final class ConsoleCapture implements AutoCloseable {

  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;
  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private final ByteArrayOutputStream err = new ByteArrayOutputStream();

  ConsoleCapture() {
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
  }

  /** Lines written to stdout, which is where {@code console.log} debug output went. */
  List<String> outLines() {
    return lines(out);
  }

  /** Lines written to stderr, which is where {@code console.error} output went. */
  List<String> errLines() {
    return lines(err);
  }

  boolean loggedOut() {
    return !outLines().isEmpty();
  }

  boolean loggedErr() {
    return !errLines().isEmpty();
  }

  /** The first stderr line, equivalent to sinon's {@code firstCall.args[0]}. */
  String firstErrLine() {
    List<String> lines = errLines();
    return lines.isEmpty() ? null : lines.get(0);
  }

  private static List<String> lines(ByteArrayOutputStream stream) {
    String text = stream.toString(StandardCharsets.UTF_8);
    if (text.isEmpty()) {
      return List.of();
    }
    List<String> lines = new ArrayList<>(Arrays.asList(text.split("\n", -1)));
    lines.remove(lines.size() - 1); // drop the empty entry after the trailing newline
    return lines;
  }

  /**
   * Runs {@code action} with the console silenced, for tests that only need the quiet and
   * never read back what was written.
   */
  @SuppressWarnings("try")
  static <T> T silenced(Supplier<T> action) {
    try (ConsoleCapture console = new ConsoleCapture()) {
      return action.get();
    }
  }

  @Override
  public void close() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }
}
