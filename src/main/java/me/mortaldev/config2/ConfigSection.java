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
 * ConfigSection rewards = config.section("scoring").section("rewards");
 * }</pre>
 *
 * <p>Typed getters come from {@link ConfigView}; paths are relative to {@link #prefix()}.
 */
public interface ConfigSection extends ConfigView {

  /**
   * The full path prefix for this section (e.g., "scoring.rewards").
   */
  String prefix();

  /**
   * The config this section belongs to.
   */
  Config config();

  /**
   * Gets a value from this section.
   * The prefix is automatically prepended to the path.
   *
   * @throws IllegalArgumentException if the path doesn't exist
   */
  <T> ConfigValue<T> get(String relativePath);

  /**
   * Updates a value in this section.
   * Returns a new config with the update applied.
   */
  <T> Config set(String relativePath, T newValue);
}
