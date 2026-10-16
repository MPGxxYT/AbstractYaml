package me.mortaldev.abstractyaml.config2;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Where a config's YAML text is stored.
 *
 * <p>{@link ConfigLoader} reads and writes YAML text through this interface, so loading and
 * rendering never touch the filesystem directly. Use {@link #file(File)} in a plugin and
 * {@link #inMemory(String)} in tests.
 */
public interface ConfigSource {

  /**
   * Returns the stored YAML text, or {@code null} if nothing has been stored yet.
   *
   * @throws UncheckedIOException if the text exists but cannot be read
   */
  String read();

  /**
   * Replaces the stored YAML text.
   *
   * @throws UncheckedIOException if the text cannot be written
   */
  void write(String yaml);

  /**
   * A short name for log and error messages, such as the file name.
   */
  String name();

  /**
   * A source backed by a file. Parent directories are created on write.
   */
  static ConfigSource file(File file) {
    return new ConfigSource() {
      @Override
      public String read() {
        if (!file.exists()) {
          return null;
        }
        try {
          return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
          throw new UncheckedIOException("Failed to read " + file, e);
        }
      }

      @Override
      public void write(String yaml) {
        try {
          File parent = file.getAbsoluteFile().getParentFile();
          if (parent != null) {
            Files.createDirectories(parent.toPath());
          }
          Files.writeString(file.toPath(), yaml, StandardCharsets.UTF_8);
        } catch (IOException e) {
          throw new UncheckedIOException("Failed to write " + file, e);
        }
      }

      @Override
      public String name() {
        return file.getName();
      }

      @Override
      public String toString() {
        return file.getPath();
      }
    };
  }

  /**
   * A source held in memory, starting with {@code initialYaml} ({@code null} for empty).
   * Writes replace the held text, so a test can load, inspect what was written, and load again.
   */
  static ConfigSource inMemory(String initialYaml) {
    return new ConfigSource() {
      private volatile String yaml = initialYaml;

      @Override
      public String read() {
        return yaml;
      }

      @Override
      public void write(String newYaml) {
        yaml = newYaml;
      }

      @Override
      public String name() {
        return "in-memory";
      }
    };
  }
}
