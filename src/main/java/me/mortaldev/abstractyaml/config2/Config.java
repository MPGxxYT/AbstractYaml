package me.mortaldev.abstractyaml.config2;

import java.io.File;
import java.util.Map;
import java.util.Optional;

/**
 * A loaded config file: one value for every path declared in its {@link ConfigSchema}.
 *
 * <p>Get one from {@link ConfigManager#get(String)} or {@link ConfigLoader#load()}, and read it
 * with the typed getters from {@link ConfigView} ({@code getInt}, {@code getStringList}, ...);
 * paths are relative to the file root.
 *
 * <p>A config is immutable, so it can be shared between threads. {@link #with(String, Object)}
 * returns a new config with a changed value; it does not change this config, the file, or the
 * config held by {@link ConfigManager}. To change a value permanently:
 *
 * <pre>{@code
 * Config updated = config.with("fireball.cooldown", 20);
 * updated.save(new File(plugin.getDataFolder(), "abilities.yml"));
 * ConfigManager.getInstance().reload("abilities");
 * }</pre>
 */
public interface Config extends ConfigView {

  /**
   * Returns the schema this config was loaded with.
   *
   * @return the schema
   */
  ConfigSchema schema();

  /**
   * Returns the schema's name ({@link ConfigSchema#name()}). If {@link RegisterConfig#value()}
   * renamed the config, this is still the schema's name, not the name used in
   * {@link ConfigManager#get(String)}.
   *
   * @return the name
   */
  String name();

  /**
   * Returns the {@link ConfigValue} at a path, with its default, comment and validator. To read
   * only the value, use a typed getter such as {@link #getInt(String)}.
   *
   * <p>{@code T} is not checked: asking for the wrong type compiles, and fails with
   * {@link ClassCastException} where the value is used.
   *
   * @param path the configuration path (e.g., "keraunos.cooldown")
   * @param <T> the Java type of the value
   * @return the config value, or empty if not found
   */
  <T> Optional<ConfigValue<T>> get(String path);

  /**
   * Returns every value in this config, keyed by full path, in schema declaration order.
   *
   * @return an unmodifiable map
   */
  Map<String, ConfigValue<?>> allValues();

  /**
   * Returns a new config with one value changed. This config is not modified.
   *
   * <p>{@code newValue} is converted with the same rules used when reading the file, so
   * {@code with("cooldown", "20")} works for an int value. The converted value must pass the
   * value's validator.
   *
   * @param path the path to update
   * @param newValue the new value, converted to the value's type
   * @param <T> the type of {@code newValue}; it does not need to match the value's type
   * @return a new config with the updated value
   * @throws IllegalArgumentException if the path doesn't exist, the value can't be converted,
   *     or it fails validation
   */
  <T> Config with(String path, T newValue);

  /**
   * Returns a new config with several values changed, applied one at a time in the map's
   * iteration order with the rules of {@link #with(String, Object)}. This config is not
   * modified. If one update fails, the exception is thrown and no config is returned.
   *
   * @param updates map of paths to new values
   * @return a new config with the updated values
   * @throws IllegalArgumentException if any path doesn't exist, or any value can't be
   *     converted or fails validation
   */
  Config withAll(Map<String, Object> updates);

  /**
   * Checks every value with its validator.
   *
   * @return the result; on failure, {@link ValidationResult#errors()} has one entry per failing
   *     value, in the form {@code "path: message"}
   */
  ValidationResult validate();

  /**
   * Renders this configuration as YAML text, with the schema's file header, section headers
   * and value comments, in declaration order. This is the same text that is written when a
   * config file is generated.
   *
   * @return the YAML text
   * @throws IllegalArgumentException if a list or map value contains something other than
   *     String, Number, Boolean, List or Map
   * @throws IllegalStateException if one path is declared both as a value and as a section
   */
  default String toYaml() {
    return YamlConfigWriter.render(this);
  }

  /**
   * Writes {@link #toYaml()} to a file, replacing its contents and creating missing parent
   * folders. The file is written as UTF-8. This does not reload anything; call
   * {@link ConfigManager#reload(String)} afterwards if the manager should use the new values.
   *
   * @param file the file to save to
   * @throws java.io.UncheckedIOException if the file cannot be written
   */
  default void save(File file) {
    ConfigSource.file(file).write(toYaml());
  }
}
