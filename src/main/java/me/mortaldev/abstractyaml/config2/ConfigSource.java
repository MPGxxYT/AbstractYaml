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
 * {@link #inMemory(String)} in tests:
 *
 * <pre>{@code
 * ConfigSource source = ConfigSource.inMemory(null);   // nothing stored yet
 * Config config = ConfigLoader.builder().schema(new AbilitiesConfigSchema())
 *     .source(source).load();
 * String generated = source.read();                     // the defaults, as YAML
 * }</pre>
 *
 * <p>To store configs somewhere else (a database, a URL), implement the three methods.
 */
public interface ConfigSource {

  /**
   * Returns the stored YAML text. A {@code null} result tells the loader the config does not
   * exist yet, so it uses the defaults and writes them.
   *
   * @return the text, or {@code null} if nothing has been stored yet
   * @throws UncheckedIOException if the text exists but cannot be read
   */
  String read();

  /**
   * Replaces the stored YAML text. The loader calls this only to create a missing config.
   *
   * @param yaml the complete new text
   * @throws UncheckedIOException if the text cannot be written
   */
  void write(String yaml);

  /**
   * Returns a short name for log and error messages, such as the file name.
   *
   * @return the name, for example {@code "abilities.yml"}
   */
  String name();

  /**
   * Returns a source backed by a file. Reads and writes use UTF-8; a write replaces the whole
   * file and creates missing parent folders. {@link #read()} returns {@code null} while the
   * file does not exist. {@link #name()} is the file name without its folder.
   *
   * @param file the file; it does not need to exist yet
   * @return the source
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
   * Returns a source held in memory, for tests. Writes replace the held text, so a test can
   * load, check what was written with {@link #read()}, and load again. Safe to use from several
   * threads. {@link #name()} is {@code "in-memory"}.
   *
   * @param initialYaml the starting text, or {@code null} to start empty, as if the file did not
   *     exist
   * @return the source
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
