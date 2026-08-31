package io.github.motdotla.dotenv.node;

import io.github.motdotla.dotenv.DotenvException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** The slice of Node's {@code fs} module that dotenv uses. */
public final class NodeFs {

  private NodeFs() {
  }

  /**
   * Reads a file and decodes it with {@code encoding}, equivalent to
   * {@code fs.readFileSync(path, { encoding })}.
   *
   * <p>{@code path} is a {@code String} or a {@code file:} {@link URI}, mirroring the
   * {@code string | URL} that Node accepts and that the {@code path} config option
   * passes straight through.
   *
   * @throws DotenvException with the code and message Node uses — {@code ENOENT},
   *     {@code EISDIR} or {@code EACCES} — when the file cannot be read
   */
  public static String readFileSync(Object path, String encoding) {
    Path file = toPath(path);
    String shown = display(path, file);

    if (Files.isDirectory(file)) {
      throw new DotenvException("EISDIR: illegal operation on a directory, read", "EISDIR");
    }

    byte[] bytes;
    try {
      bytes = Files.readAllBytes(file);
    } catch (NoSuchFileException e) {
      throw new DotenvException(
          "ENOENT: no such file or directory, open '" + shown + "'", "ENOENT");
    } catch (AccessDeniedException e) {
      throw new DotenvException("EACCES: permission denied, open '" + shown + "'", "EACCES");
    } catch (IOException e) {
      throw new DotenvException("EIO: i/o error, read '" + shown + "'", "EIO");
    }

    return NodeEncoding.decode(bytes, encoding);
  }

  private static Path toPath(Object path) {
    if (path instanceof URI uri) {
      return Paths.get(uri);
    }
    if (path instanceof Path filePath) {
      return filePath;
    }
    if (path instanceof String string) {
      return Paths.get(string);
    }
    throw new IllegalArgumentException(
        "The \"path\" argument must be of type string or an instance of URI. Received "
            + (path == null ? "null" : "an instance of " + path.getClass().getSimpleName()));
  }

  /** Node reports the path as the caller wrote it, except for URLs, which it converts first. */
  private static String display(Object path, Path file) {
    return path instanceof String string ? string : file.toString();
  }
}
