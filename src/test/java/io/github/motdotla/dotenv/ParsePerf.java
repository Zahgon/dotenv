package io.github.motdotla.dotenv;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Performance check for {@link Dotenv#parse(String)}.
 *
 * <p>Not run as part of {@code mvn test} (it has no assertions); invoke it directly:
 *
 * <pre>{@code
 * mvn -q test-compile exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=io.github.motdotla.dotenv.ParsePerf
 * }</pre>
 *
 * <p>Reports the median ms over 7 runs of 5000 parse calls on a representative {@code .env},
 * for both parsers. This consolidates the original's two identical-in-purpose scripts,
 * {@code scripts/parse-perf.js} and {@code tests/test-parse-perf.js}, into one benchmark
 * covering both.
 */
public final class ParsePerf {

  private ParsePerf() {
  }

  private static final int N = 5000;

  private static final String SAMPLE = String.join("\n",
      "# Database",
      "DATABASE_URL=postgresql://user:password@localhost:5432/mydb?schema=public",
      "REDIS_URL=redis://default:password@localhost:6379",
      "",
      "# Auth",
      "JWT_SECRET=verylongrandomstringthatlookslikeasecretsharedacrossservices",
      "OAUTH_GOOGLE_CLIENT_ID=1234567890-abcdefg.apps.googleusercontent.com",
      "OAUTH_GITHUB_CLIENT_SECRET=ghp_abcdefghijklmnopqrstuvwxyz",
      "",
      "# AWS",
      "AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE",
      "AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
      "S3_BUCKET=my-app-uploads-prod",
      "",
      "# Quoted / multiline",
      "EMAIL_FROM=\"MyApp <noreply@myapp.com>\"",
      "ALLOWED_ORIGINS=\"https://myapp.com,https://www.myapp.com\"",
      "MULTILINE_KEY=\"line one\\nline two\\nline three\"",
      "",
      "# Misc",
      "NODE_ENV=production",
      "PORT=3000",
      "LOG_LEVEL=info",
      "FEATURE_FLAG_A=true",
      "").repeat(8);

  public static void main(String[] args) {
    byte[] buf = SAMPLE.getBytes(StandardCharsets.UTF_8);

    bench("parse()", buf, null);
    bench("parse(fast)", buf, new ParseOptions().fast(true));
  }

  private static void bench(String label, byte[] buf, ParseOptions options) {
    for (int i = 0; i < 200; i++) {
      Dotenv.parse(buf, options);
    }

    double[] runs = new double[7];
    for (int r = 0; r < runs.length; r++) {
      long start = System.nanoTime();
      for (int i = 0; i < N; i++) {
        Dotenv.parse(buf, options);
      }
      runs[r] = (System.nanoTime() - start) / 1e6;
    }
    Arrays.sort(runs);
    System.out.printf(
        Locale.ROOT, "%s x %d: median %.2f ms%n", label, N, runs[runs.length / 2]);
  }
}
