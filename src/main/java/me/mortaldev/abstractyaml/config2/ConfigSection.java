package me.mortaldev.abstractyaml.config2;

/**
 * A view of the values under one path prefix of a {@link Config}. Get one from
 * {@link ConfigView#section(String)} or {@link ConfigManager#getSection(String, String)}.
 *
 * <pre>{@code
 * ConfigSection keraunos = config.section("keraunos");
 * int cooldown = keraunos.getInt("cooldown");  // Accesses "keraunos.cooldown"
 * double radius = keraunos.getDouble("radius");  // Accesses "keraunos.radius"
 * ConfigSection rewards = config.section("scoring").section("rewards");
 * }</pre>
 *
 * <p>Typed getters come from {@link ConfigView}; paths are relative to {@link #prefix()}. Error
 * messages show the full path. A section reads from the config it was created from; after a
 * reload, get a new section.
 */
public interface ConfigSection extends ConfigView {

  /**
   * Returns the full path prefix of this section, without a trailing dot.
   *
   * @return the prefix (e.g., "scoring.rewards")
   */
  String prefix();

  /**
   * Returns the config this section reads from.
   *
   * @return the config
   */
  Config config();

  /**
   * Returns the {@link ConfigValue} at a path relative to this section. Same as
   * {@link #getOrThrow(String)}.
   *
   * @param relativePath the path below {@link #prefix()}, such as {@code "cooldown"}
   * @param <T> the Java type of the value; not checked
   * @return the config value
   * @throws IllegalArgumentException if no value is declared at the full path
   */
  <T> ConfigValue<T> get(String relativePath);

  /**
   * Returns a new config with one value in this section changed, using the rules of
   * {@link Config#with(String, Object)}. Neither this section nor its config is modified; the
   * returned value is a {@link Config}, not a section.
   *
   * @param relativePath the path below {@link #prefix()}, such as {@code "cooldown"}
   * @param newValue the new value, converted to the value's type
   * @param <T> the type of {@code newValue}
   * @return a new config with the updated value
   * @throws IllegalArgumentException if the path doesn't exist, the value can't be converted,
   *     or it fails validation
   */
  <T> Config set(String relativePath, T newValue);
}
