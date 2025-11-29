package me.mortaldev.config2;

/**
 * A view of a config with a path prefix.
 * Useful for organizing related configuration values.
 *
 * <p>Example:
 * <pre>{@code
 * ConfigSection keraunos = config.section("keraunos");
 * int cooldown = keraunos.getInt("cooldown");  // Accesses "keraunos.cooldown"
 * double radius = keraunos.getDouble("radius");  // Accesses "keraunos.radius"
 * }</pre>
 */
public interface ConfigSection {

  /**
   * The path prefix for this section.
   */
  String prefix();

  /**
   * The parent config.
   */
  Config config();

  /**
   * Gets a value from this section.
   * The prefix is automatically prepended to the path.
   */
  <T> ConfigValue<T> get(String relativePath);

  /**
   * Gets an integer value.
   */
  int getInt(String relativePath);

  /**
   * Gets a double value.
   */
  double getDouble(String relativePath);

  /**
   * Gets a string value.
   */
  String getString(String relativePath);

  /**
   * Gets a boolean value.
   */
  boolean getBoolean(String relativePath);

  /**
   * Gets a string list.
   */
  java.util.List<String> getStringList(String relativePath);

  /**
   * Gets an integer list.
   */
  java.util.List<Integer> getIntList(String relativePath);

  /**
   * Gets a double list.
   */
  java.util.List<Double> getDoubleList(String relativePath);

  /**
   * Updates a value in this section.
   * Returns a new config with the update applied.
   */
  <T> Config set(String relativePath, T newValue);
}
