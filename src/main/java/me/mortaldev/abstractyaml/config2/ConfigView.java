package me.mortaldev.abstractyaml.config2;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read access to config values by path. Implemented by {@link Config} (paths from the file
 * root) and {@link ConfigSection} (paths relative to a section prefix).
 *
 * <pre>{@code
 * Config config = ConfigManager.getInstance().get("abilities");
 * int cooldown = config.getInt("fireball.cooldown");
 *
 * ConfigSection fireball = config.section("fireball");
 * int sameCooldown = fireball.getInt("cooldown");
 * }</pre>
 *
 * <p>Use the getter that matches the type declared in the schema: a value declared with
 * {@code intValue} is read with {@link #getInt(String)}, one declared with
 * {@code stringListValue} with {@link #getStringList(String)}, and so on. The typed getters
 * never convert between types; {@code getDouble} on an int value throws.
 *
 * <p>Every getter returns the value loaded from the file, or the default when the file had no
 * usable value. Lists and maps loaded from a file are unmodifiable. A view never changes after
 * it is created; after a reload, get a new {@link Config} from {@link ConfigManager}.
 *
 * <p>Implementations provide {@link #getOrThrow(String)}, {@link #getValue(String)} and
 * {@link #section(String)}; the typed getters are built on {@code getOrThrow}.
 */
public interface ConfigView {

  /**
   * Returns the {@link ConfigValue} at a path, with its default, comment and validator. Most
   * code uses a typed getter such as {@link #getInt(String)} instead.
   *
   * <p>{@code T} is not checked: asking for the wrong type compiles, and fails with
   * {@link ClassCastException} where the value is used.
   *
   * @param path the configuration path
   * @param <T> the Java type of the value
   * @return the config value
   * @throws IllegalArgumentException if no value is declared at the path; the message is
   *     {@code "No config value at path: <full path>"}
   */
  <T> ConfigValue<T> getOrThrow(String path);

  /**
   * Returns the value at a path, or an empty {@code Optional} when nothing is declared there.
   * Use this when the path may not exist, for example a path built from user input.
   *
   * <p>{@code T} is not checked: asking for the wrong type compiles, and fails with
   * {@link ClassCastException} where the value is used.
   *
   * @param path the configuration path
   * @param <T> the Java type of the value
   * @return the value, or empty if not found
   */
  <T> Optional<T> getValue(String path);

  /**
   * Returns a view of the values under a prefix, relative to this view. Paths passed to the
   * section's getters are appended to the prefix with a dot.
   *
   * <pre>{@code
   * ConfigSection rewards = config.section("scoring.rewards");
   * rewards.getInt("win");                          // reads "scoring.rewards.win"
   * config.section("scoring").section("rewards");   // the same section
   * }</pre>
   *
   * <p>The prefix is not checked when the section is created; a wrong prefix shows up as an
   * {@link IllegalArgumentException} from the first getter.
   *
   * @param prefix the path prefix (e.g., "keraunos"), without a trailing dot
   * @return a config section
   */
  ConfigSection section(String prefix);

  /**
   * Returns a whole number declared with {@code intValue} or {@link ConfigValue.Int}.
   *
   * @param path the configuration path
   * @return the value
   * @throws IllegalArgumentException if nothing is declared at the path, or the value there is
   *     not an int value
   */
  default int getInt(String path) {
    return typed(path, ConfigValue.Int.class, "an integer");
  }

  /**
   * Returns a decimal number declared with {@code doubleValue} or {@link ConfigValue.Double}.
   * An int value is not converted; read it with {@link #getInt(String)}.
   *
   * @param path the configuration path
   * @return the value
   * @throws IllegalArgumentException if nothing is declared at the path, or the value there is
   *     not a double value
   */
  default double getDouble(String path) {
    return typed(path, ConfigValue.Double.class, "a double");
  }

  /**
   * Returns a text value declared with {@code stringValue} or {@link ConfigValue.String}.
   *
   * @param path the configuration path
   * @return the value
   * @throws IllegalArgumentException if nothing is declared at the path, or the value there is
   *     not a string value
   */
  default String getString(String path) {
    return typed(path, ConfigValue.String.class, "a string");
  }

  /**
   * Returns a true/false value declared with {@code boolValue} or {@link ConfigValue.Boolean}.
   *
   * @param path the configuration path
   * @return the value
   * @throws IllegalArgumentException if nothing is declared at the path, or the value there is
   *     not a boolean value
   */
  default boolean getBoolean(String path) {
    return typed(path, ConfigValue.Boolean.class, "a boolean");
  }

  /**
   * Returns a text list declared with {@code stringListValue} or
   * {@link ConfigValue.StringList}.
   *
   * @param path the configuration path
   * @return the list; unmodifiable when loaded from a file
   * @throws IllegalArgumentException if nothing is declared at the path, or the value there is
   *     not a string list
   */
  default List<String> getStringList(String path) {
    return typed(path, ConfigValue.StringList.class, "a string list");
  }

  /**
   * Returns a whole-number list declared with {@code intList}, {@code intListValue} or
   * {@link ConfigValue.IntList}.
   *
   * @param path the configuration path
   * @return the list; unmodifiable when loaded from a file
   * @throws IllegalArgumentException if nothing is declared at the path, or the value there is
   *     not an integer list
   */
  default List<Integer> getIntList(String path) {
    return typed(path, ConfigValue.IntList.class, "an integer list");
  }

  /**
   * Returns a decimal list declared with {@code doubleList}, {@code doubleListValue} or
   * {@link ConfigValue.DoubleList}.
   *
   * @param path the configuration path
   * @return the list; unmodifiable when loaded from a file
   * @throws IllegalArgumentException if nothing is declared at the path, or the value there is
   *     not a double list
   */
  default List<Double> getDoubleList(String path) {
    return typed(path, ConfigValue.DoubleList.class, "a double list");
  }

  /**
   * Returns a list of objects declared with {@code mapListValue} or
   * {@link ConfigValue.MapList}. Field values are plain YAML data (String, Integer, Double,
   * Boolean, List or Map); cast each one to the type you expect.
   *
   * @param path the configuration path
   * @return the list; unmodifiable, with unmodifiable maps, when loaded from a file
   * @throws IllegalArgumentException if nothing is declared at the path, or the value there is
   *     not a map list
   */
  default List<Map<String, Object>> getMapList(String path) {
    return typed(path, ConfigValue.MapList.class, "a map list");
  }

  /**
   * Returns an object declared with {@code mapValue} or {@link ConfigValue.MapValue}. Field
   * values are plain YAML data (String, Integer, Double, Boolean, List or Map); cast each one
   * to the type you expect.
   *
   * @param path the configuration path
   * @return the map in file order; unmodifiable when loaded from a file
   * @throws IllegalArgumentException if nothing is declared at the path, or the value there is
   *     not a map value. A {@code stringMapValue} or {@code intMapValue} is not a map value; read
   *     those with {@link #getStringMap(String)} or {@link #getIntMap(String)}.
   */
  default Map<String, Object> getMap(String path) {
    return typed(path, ConfigValue.MapValue.class, "a map");
  }

  /**
   * Returns a text-to-text map declared with {@code stringMapValue} or
   * {@link ConfigValue.StringMap}.
   *
   * @param path the configuration path
   * @return the map in file order; unmodifiable when loaded from a file
   * @throws IllegalArgumentException if nothing is declared at the path, or the value there is
   *     not a string map
   */
  default Map<String, String> getStringMap(String path) {
    return typed(path, ConfigValue.StringMap.class, "a string map");
  }

  /**
   * Returns a text-to-whole-number map declared with {@code intMapValue} or
   * {@link ConfigValue.IntMap}.
   *
   * @param path the configuration path
   * @return the map in file order; unmodifiable when loaded from a file
   * @throws IllegalArgumentException if nothing is declared at the path, or the value there is
   *     not an integer map
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
