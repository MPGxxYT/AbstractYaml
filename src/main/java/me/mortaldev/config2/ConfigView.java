package me.mortaldev.config2;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read access to config values by path. Implemented by {@link Config} (paths from the file
 * root) and {@link ConfigSection} (paths relative to a section prefix).
 *
 * <p>Implementations provide {@link #getOrThrow(String)}, {@link #getValue(String)} and
 * {@link #section(String)}; the typed getters are built on {@code getOrThrow}.
 */
public interface ConfigView {

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
   * Creates a section view with a path prefix, relative to this view.
   * This is useful for organizing related values.
   *
   * @param prefix the path prefix (e.g., "keraunos")
   * @return a config section
   */
  ConfigSection section(String prefix);

  /**
   * Gets an integer value.
   *
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  default int getInt(String path) {
    return typed(path, ConfigValue.Int.class, "an integer");
  }

  /**
   * Gets a double value.
   *
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  default double getDouble(String path) {
    return typed(path, ConfigValue.Double.class, "a double");
  }

  /**
   * Gets a string value.
   *
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  default String getString(String path) {
    return typed(path, ConfigValue.String.class, "a string");
  }

  /**
   * Gets a boolean value.
   *
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  default boolean getBoolean(String path) {
    return typed(path, ConfigValue.Boolean.class, "a boolean");
  }

  /**
   * Gets a string list.
   *
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  default List<String> getStringList(String path) {
    return typed(path, ConfigValue.StringList.class, "a string list");
  }

  /**
   * Gets an integer list.
   *
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  default List<Integer> getIntList(String path) {
    return typed(path, ConfigValue.IntList.class, "an integer list");
  }

  /**
   * Gets a double list.
   *
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  default List<Double> getDoubleList(String path) {
    return typed(path, ConfigValue.DoubleList.class, "a double list");
  }

  /**
   * Gets a list of maps.
   *
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  default List<Map<String, Object>> getMapList(String path) {
    return typed(path, ConfigValue.MapList.class, "a map list");
  }

  /**
   * Gets a map with values of any YAML type.
   *
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  default Map<String, Object> getMap(String path) {
    return typed(path, ConfigValue.MapValue.class, "a map");
  }

  /**
   * Gets a string-to-string map.
   *
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  default Map<String, String> getStringMap(String path) {
    return typed(path, ConfigValue.StringMap.class, "a string map");
  }

  /**
   * Gets a string-to-integer map.
   *
   * @throws IllegalArgumentException if the path doesn't exist or is wrong type
   */
  default Map<String, Integer> getIntMap(String path) {
    return typed(path, ConfigValue.IntMap.class, "an integer map");
  }

  private <R, V extends ConfigValue<R>> R typed(String path, Class<V> kind, String description) {
    ConfigValue<?> value = getOrThrow(path);
    if (!kind.isInstance(value)) {
      throw new IllegalArgumentException("Value at " + value.path() + " is not " + description);
    }
    return kind.cast(value).value();
  }
}
