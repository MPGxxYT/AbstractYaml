package me.mortaldev.config2;

import java.io.File;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable configuration container.
 * Config instances are thread-safe and cannot be modified after creation.
 * Use {@link #with(String, Object)} to create a new config with updated values.
 *
 * <p>Typed getters such as {@link #getInt(String)} come from {@link ConfigView}; paths are
 * relative to the file root.
 */
public interface Config extends ConfigView {

  /**
   * Returns the schema that defines this configuration.
   */
  ConfigSchema schema();

  /**
   * Returns the name of this configuration.
   */
  String name();

  /**
   * Gets a configuration value by path.
   *
   * @param path the configuration path (e.g., "keraunos.cooldown")
   * @return the config value, or empty if not found
   */
  <T> Optional<ConfigValue<T>> get(String path);

  /**
   * Returns all configuration values, in schema declaration order.
   */
  Map<String, ConfigValue<?>> allValues();

  /**
   * Creates a new config with an updated value.
   * The original config is not modified.
   *
   * @param path the path to update
   * @param newValue the new value, converted to the value's type
   * @return a new config with the updated value
   * @throws IllegalArgumentException if the path doesn't exist, the value can't be converted,
   *     or it fails validation
   */
  <T> Config with(String path, T newValue);

  /**
   * Creates a new config with multiple updated values.
   *
   * @param updates map of paths to new values
   * @return a new config with the updated values
   */
  Config withAll(Map<String, Object> updates);

  /**
   * Validates all configuration values.
   *
   * @return the validation result
   */
  ValidationResult validate();

  /**
   * Renders this configuration as YAML text, with the schema's comments, in declaration order.
   */
  default String toYaml() {
    return YamlConfigWriter.render(this);
  }

  /**
   * Saves this configuration to a file.
   *
   * @param file the file to save to
   */
  default void save(File file) {
    ConfigSource.file(file).write(toYaml());
  }
}
