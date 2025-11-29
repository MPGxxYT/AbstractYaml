package me.mortaldev.config2;

import java.io.File;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable configuration container.
 * Config instances are thread-safe and cannot be modified after creation.
 * Use {@link #with(String, Object)} to create a new config with updated values.
 */
public interface Config {

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
   * Gets a configuration value by path, throwing an exception if not found.
   *
   * @param path the configuration path
   * @return the config value
   * @throws IllegalArgumentException if the path doesn't exist
   */
  <T> ConfigValue<T> getOrThrow(String path);

  /**
   * Gets just the value (not the ConfigValue wrapper) by path.
   *
   * @param path the configuration path
   * @return the value, or empty if not found
   */
  <T> Optional<T> getValue(String path);

  /**
   * Gets an integer value.
   *
   * @param path the configuration path
   * @return the integer value
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  int getInt(String path);

  /**
   * Gets a double value.
   *
   * @param path the configuration path
   * @return the double value
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  double getDouble(String path);

  /**
   * Gets a string value.
   *
   * @param path the configuration path
   * @return the string value
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  String getString(String path);

  /**
   * Gets a boolean value.
   *
   * @param path the configuration path
   * @return the boolean value
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  boolean getBoolean(String path);

  /**
   * Gets a string list.
   *
   * @param path the configuration path
   * @return the string list
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  java.util.List<String> getStringList(String path);

  /**
   * Gets an integer list.
   *
   * @param path the configuration path
   * @return the integer list
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  java.util.List<Integer> getIntList(String path);

  /**
   * Gets a double list.
   *
   * @param path the configuration path
   * @return the double list
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  java.util.List<Double> getDoubleList(String path);

  /**
   * Returns all configuration values.
   */
  Map<String, ConfigValue<?>> allValues();

  /**
   * Creates a new config with an updated value.
   * The original config is not modified.
   *
   * @param path the path to update
   * @param newValue the new value
   * @return a new config with the updated value
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
   * Saves this configuration to a file.
   *
   * @param file the file to save to
   */
  void save(File file);

  /**
   * Creates a section view of this config with a path prefix.
   * This is useful for organizing related values.
   *
   * @param prefix the path prefix (e.g., "keraunos")
   * @return a config section
   */
  ConfigSection section(String prefix);
}
