package me.mortaldev.abstractyaml.config2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Declares the contents of one config file: every value, its default, its comment and its
 * validator. Subclass it once per file and declare the values in the constructor.
 *
 * <pre>{@code
 * @RegisterConfig(priority = 10)
 * public class AbilitiesConfigSchema extends ConfigSchema {
 *     public AbilitiesConfigSchema() {
 *         super("abilities");                       // abilities.yml
 *         header("Ability settings");
 *
 *         section("fireball")
 *             .header("The fireball ability")
 *             .intValue("cooldown", 10, Validator.range(1, 300), "Seconds between casts")
 *             .doubleValue("radius", 3.0, "Explosion radius in blocks")
 *             .stringListValue("allowed-items", List.of("BLAZE_ROD"), "Items that cast it");
 *
 *         value(new ConfigValue.Boolean("debug", false, "Log every cast"));
 *     }
 * }
 * }</pre>
 *
 * <p>This produces:
 *
 * <pre>
 * # Ability settings
 *
 * # The fireball ability
 * fireball:
 *   cooldown: 10                          # Seconds between casts
 *   radius: 3.0                           # Explosion radius in blocks
 *   allowed-items:  # Items that cast it
 *     - "BLAZE_ROD"
 *
 * debug: false                            # Log every cast
 * </pre>
 *
 * <p>Values appear in the file in the order they are declared. Load the schema with
 * {@link ConfigLoader} or register it with {@link ConfigManager}.
 *
 * <p><b>Requirements:</b>
 * <ul>
 *   <li>Every default must pass its own validator; otherwise loading fails with
 *       {@link ConfigLoader.ConfigLoadException}.
 *   <li>To register the class with {@link ConfigManager#register(Class)} or
 *       {@link ConfigRegistry#register(Class)}, it needs {@link RegisterConfig} and a public
 *       no-argument constructor.
 *   <li>For {@link GenerateWrapper}, keys and section names must be string literals or
 *       {@code static final String} constants, because the wrapper generator reads the
 *       constructor's source code instead of running it.
 * </ul>
 *
 * <p>A schema is filled in once by its constructor and only read afterwards. Its methods are
 * not synchronized, so don't declare values from other threads.
 */
public abstract class ConfigSchema {
  private final String name;
  private final Map<String, ConfigValue<?>> values;
  private final Map<String, String> sectionHeaders;
  private String fileHeader;

  /**
   * Starts a schema with the given config name.
   *
   * <p>The name identifies the config in {@link ConfigManager#get(String)} and, unless
   * {@link RegisterConfig} or {@link ConfigLoader.Builder#file} says otherwise, sets the file
   * name: {@code "abilities"} is stored in {@code abilities.yml} in the plugin's data folder.
   *
   * @param name the config name, without the {@code .yml} extension
   */
  protected ConfigSchema(String name) {
    this.name = name;
    this.values = new LinkedHashMap<>();
    this.sectionHeaders = new LinkedHashMap<>();
  }

  /**
   * Returns the config name passed to the constructor. {@link RegisterConfig#value()} can give
   * the config a different name in {@link ConfigManager}; this method still returns the
   * constructor's name.
   *
   * @return the name, without the {@code .yml} extension
   */
  public String name() {
    return name;
  }

  /**
   * Returns every value declared in this schema, with its default, in declaration order.
   *
   * @return an unmodifiable copy; later declarations do not change it
   */
  public Set<ConfigValue<?>> values() {
    return Collections.unmodifiableSet(new LinkedHashSet<>(values.values()));
  }

  /**
   * Returns the value declared at a path, with its default. To read the loaded value, use
   * {@link Config#get(String)} or a typed getter such as {@link ConfigView#getInt(String)}.
   *
   * @param path the full dotted path, such as {@code "fireball.cooldown"}
   * @return the declared value, or {@code null} if nothing is declared at {@code path}
   */
  public ConfigValue<?> getValue(String path) {
    return values.get(path);
  }

  /**
   * Checks whether a value is declared at a path. Sections are not values:
   * {@code hasValue("fireball")} is {@code false} when only {@code "fireball.cooldown"} is
   * declared.
   *
   * @param path the full dotted path, such as {@code "fireball.cooldown"}
   * @return {@code true} if a value is declared at exactly {@code path}
   */
  public boolean hasValue(String path) {
    return values.containsKey(path);
  }

  /**
   * Returns the comment written at the top of the file, set with {@link #header(String)}.
   *
   * @return the header text, or {@code null} if none was set
   */
  public String fileHeader() {
    return fileHeader;
  }

  /**
   * Returns the comment written above a section, set with
   * {@code section(path).header(...)}.
   *
   * @param sectionPath the full section path (e.g., "keraunos" or "scoring.rewards")
   * @return the header text, or {@code null} if none was set
   */
  public String sectionHeader(String sectionPath) {
    return sectionHeaders.get(sectionPath);
  }

  /**
   * Declares a value. Call this from your schema's constructor, or use {@link #section(String)}
   * for values that share a prefix.
   *
   * <pre>{@code
   * value(new ConfigValue.Int("max-players", 16, Validator.min(2), "Players per game"));
   * }</pre>
   *
   * <p>Declaring the same path twice replaces the first declaration but keeps its position in
   * the file.
   *
   * @param configValue the value with its full dotted path and default
   * @param <T> the Java type of the value
   */
  protected <T> void value(ConfigValue<T> configValue) {
    values.put(configValue.path(), configValue);
  }

  /**
   * Sets the comment written at the top of the file, followed by a blank line. Each line of
   * {@code header} becomes a {@code # } comment line; lines that already start with {@code #}
   * are kept as they are, and blank lines become a lone {@code #}. Calling this again replaces
   * the header.
   *
   * @param header the comment text, using {@code \n} between lines; {@code null} or empty for
   *     none
   */
  protected void header(String header) {
    this.fileHeader = header;
  }

  /**
   * Starts declaring values under a common prefix. Every key passed to the returned builder is
   * placed under {@code prefix}: {@code section("fireball").intValue("cooldown", ...)} declares
   * {@code "fireball.cooldown"}.
   *
   * <p>{@code prefix} may contain dots ({@code "scoring.rewards"}). Calling {@code section} with
   * the same prefix again adds to the same section in the file.
   *
   * @param prefix the section path, without a trailing dot
   * @return a builder for that section
   */
  protected SectionBuilder section(String prefix) {
    return new SectionBuilder(prefix);
  }

  /**
   * Declares values under one section path. Get one from {@link ConfigSchema#section(String)}.
   *
   * <p>Every method returns a builder so calls can be chained. The value methods and
   * {@link #header(String)} return this builder; {@link #section(String)} returns a builder for a
   * nested section, and {@link #parent()} goes back up one level:
   *
   * <pre>{@code
   * section("game")
   *     .intValue("max-players", 16, "Players per game")
   *     .section("rewards")                 // game.rewards
   *         .intValue("win", 100, "Coins for a win")
   *         .parent()                       // back to game
   *     .boolValue("teams", true, "Split players into teams");
   * }</pre>
   *
   * <p>In every value method, {@code key} is relative to this section and must not start or end
   * with a dot; {@code comment} is written after the value in the file and may be {@code null}.
   * A default that fails its validator makes loading fail.
   */
  protected class SectionBuilder {
    private final String prefix;
    private final SectionBuilder parent;

    private SectionBuilder(String prefix) {
      this(prefix, null);
    }

    private SectionBuilder(String prefix, SectionBuilder parent) {
      this.prefix = prefix;
      this.parent = parent;
    }

    /**
     * Sets the comment written above this section's key in the file. Multi-line text works the
     * same way as {@link ConfigSchema#header(String)}. Calling this again replaces the header.
     *
     * @param header the comment text, using {@code \n} between lines
     * @return this builder
     */
    public SectionBuilder header(String header) {
      sectionHeaders.put(prefix, header);
      return this;
    }

    /**
     * Declares a whole number ({@link ConfigValue.Int}), read with
     * {@link ConfigView#getInt(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value
     * @param comment the comment written after the value, or {@code null}
     * @return this builder
     */
    public SectionBuilder intValue(String key, int defaultValue, String comment) {
      value(new ConfigValue.Int(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Declares a whole number ({@link ConfigValue.Int}) with a validator, read with
     * {@link ConfigView#getInt(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; must pass {@code validator}
     * @param validator checks the value, for example {@code Validator.range(1, 300)}
     * @param comment the comment written after the value, or {@code null}
     * @return this builder
     */
    public SectionBuilder intValue(
        String key, int defaultValue, Validator<Integer> validator, String comment) {
      value(new ConfigValue.Int(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Declares a decimal number ({@link ConfigValue.Double}), read with
     * {@link ConfigView#getDouble(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value
     * @param comment the comment written after the value, or {@code null}
     * @return this builder
     */
    public SectionBuilder doubleValue(String key, double defaultValue, String comment) {
      value(new ConfigValue.Double(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Declares a decimal number ({@link ConfigValue.Double}) with a validator, read with
     * {@link ConfigView#getDouble(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; must pass {@code validator}
     * @param validator checks the value, for example {@code Validator.min(0.0)}. Use double
     *     literals: {@code Validator.min(0)} picks the integer overload and does not compile here.
     * @param comment the comment written after the value, or {@code null}
     * @return this builder
     */
    public SectionBuilder doubleValue(
        String key, double defaultValue, Validator<Double> validator, String comment) {
      value(new ConfigValue.Double(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Declares a text value ({@link ConfigValue.String}), read with
     * {@link ConfigView#getString(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value
     * @param comment the comment written after the value, or {@code null}
     * @return this builder
     */
    public SectionBuilder stringValue(String key, String defaultValue, String comment) {
      value(new ConfigValue.String(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Declares a text value ({@link ConfigValue.String}) with a validator, read with
     * {@link ConfigView#getString(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; must pass {@code validator}
     * @param validator checks the value, for example {@link Validator#notEmpty()}
     * @param comment the comment written after the value, or {@code null}
     * @return this builder
     */
    public SectionBuilder stringValue(
        String key, String defaultValue, Validator<String> validator, String comment) {
      value(new ConfigValue.String(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Declares a true/false value ({@link ConfigValue.Boolean}), read with
     * {@link ConfigView#getBoolean(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value
     * @param comment the comment written after the value, or {@code null}
     * @return this builder
     */
    public SectionBuilder boolValue(String key, boolean defaultValue, String comment) {
      value(new ConfigValue.Boolean(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Declares a true/false value ({@link ConfigValue.Boolean}) with a validator, read with
     * {@link ConfigView#getBoolean(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; must pass {@code validator}
     * @param validator checks the value
     * @param comment the comment written after the value, or {@code null}
     * @return this builder
     */
    public SectionBuilder boolValue(
        String key, boolean defaultValue, Validator<Boolean> validator, String comment) {
      value(new ConfigValue.Boolean(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Declares a text list ({@link ConfigValue.StringList}), read with
     * {@link ConfigView#getStringList(String)}. Same as
     * {@link #stringListValue(String, java.util.List, String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; pass {@code List.of(...)}
     * @param comment the comment written after the key, or {@code null}
     * @return this builder
     */
    public SectionBuilder stringList(
        String key, java.util.List<String> defaultValue, String comment) {
      value(new ConfigValue.StringList(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Declares a text list ({@link ConfigValue.StringList}) with a validator, read with
     * {@link ConfigView#getStringList(String)}. Same as
     * {@link #stringListValue(String, java.util.List, Validator, String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; pass {@code List.of(...)}; must pass
     *     {@code validator}
     * @param validator checks the value, for example {@link Validator#allStringsNotEmpty()}
     * @param comment the comment written after the key, or {@code null}
     * @return this builder
     */
    public SectionBuilder stringList(
        String key,
        java.util.List<String> defaultValue,
        Validator<java.util.List<String>> validator,
        String comment) {
      value(new ConfigValue.StringList(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Declares a whole-number list ({@link ConfigValue.IntList}), read with
     * {@link ConfigView#getIntList(String)}. To add a validator, use
     * {@link #intListValue(String, java.util.List, Validator, String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; pass {@code List.of(...)}
     * @param comment the comment written after the value, or {@code null}
     * @return this builder
     */
    public SectionBuilder intList(
        String key, java.util.List<Integer> defaultValue, String comment) {
      value(new ConfigValue.IntList(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Declares a decimal list ({@link ConfigValue.DoubleList}), read with
     * {@link ConfigView#getDoubleList(String)}. To add a validator, use
     * {@link #doubleListValue(String, java.util.List, Validator, String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; pass {@code List.of(...)}
     * @param comment the comment written after the value, or {@code null}
     * @return this builder
     */
    public SectionBuilder doubleList(
        String key, java.util.List<java.lang.Double> defaultValue, String comment) {
      value(new ConfigValue.DoubleList(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Declares a whole-number list ({@link ConfigValue.IntList}) with a validator, read with
     * {@link ConfigView#getIntList(String)}. Without a validator, use
     * {@link #intList(String, java.util.List, String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; pass {@code List.of(...)}; must pass
     *     {@code validator}
     * @param validator checks the value, for example {@code Validator.allIntegersInRange(1, 100)}
     * @param comment the comment written after the value, or {@code null}
     * @return this builder
     */
    public SectionBuilder intListValue(
        String key,
        java.util.List<Integer> defaultValue,
        Validator<java.util.List<Integer>> validator,
        String comment) {
      value(new ConfigValue.IntList(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Declares a decimal list ({@link ConfigValue.DoubleList}) with a validator, read with
     * {@link ConfigView#getDoubleList(String)}. Without a validator, use
     * {@link #doubleList(String, java.util.List, String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; pass {@code List.of(...)}; must pass
     *     {@code validator}
     * @param validator checks the value, for example
     *     {@code Validator.allDoublesInRange(0.0, 10.0)}
     * @param comment the comment written after the value, or {@code null}
     * @return this builder
     */
    public SectionBuilder doubleListValue(
        String key,
        java.util.List<java.lang.Double> defaultValue,
        Validator<java.util.List<java.lang.Double>> validator,
        String comment) {
      value(new ConfigValue.DoubleList(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Declares a text list ({@link ConfigValue.StringList}), read with
     * {@link ConfigView#getStringList(String)}. Same as
     * {@link #stringList(String, java.util.List, String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; pass {@code List.of(...)}
     * @param comment the comment written after the key, or {@code null}
     * @return this builder
     */
    public SectionBuilder stringListValue(
        String key, java.util.List<String> defaultValue, String comment) {
      value(new ConfigValue.StringList(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Declares a text list ({@link ConfigValue.StringList}) with a validator, read with
     * {@link ConfigView#getStringList(String)}. Same as
     * {@link #stringList(String, java.util.List, Validator, String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; pass {@code List.of(...)}; must pass
     *     {@code validator}
     * @param validator checks the value, for example {@link Validator#allStringsNotEmpty()}
     * @param comment the comment written after the key, or {@code null}
     * @return this builder
     */
    public SectionBuilder stringListValue(
        String key,
        java.util.List<String> defaultValue,
        Validator<java.util.List<String>> validator,
        String comment) {
      value(new ConfigValue.StringList(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Declares a list of objects ({@link ConfigValue.MapList}), read with
     * {@link ConfigView#getMapList(String)}. Each object is a map of field names to values:
     *
     * <pre>{@code
     * .mapListValue("points", List.of(
     *     Map.of("name", "a", "radius", 3.0),
     *     Map.of("name", "b", "radius", 5.0)), "Capture points")
     * }</pre>
     *
     * @param key the key within this section
     * @param defaultValue the default value; use only String, Number, Boolean, List and Map
     *     inside, or writing the file fails
     * @param comment the comment written after the key, or {@code null}
     * @return this builder
     */
    public SectionBuilder mapListValue(
        String key,
        java.util.List<java.util.Map<String, Object>> defaultValue,
        String comment) {
      value(new ConfigValue.MapList(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Declares a list of objects ({@link ConfigValue.MapList}) with a validator, read with
     * {@link ConfigView#getMapList(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; use only String, Number, Boolean, List and Map
     *     inside, or writing the file fails; must pass {@code validator}
     * @param validator checks the value, for example {@code Validator.minSize(1)}
     * @param comment the comment written after the key, or {@code null}
     * @return this builder
     */
    public SectionBuilder mapListValue(
        String key,
        java.util.List<java.util.Map<String, Object>> defaultValue,
        Validator<java.util.List<java.util.Map<String, Object>>> validator,
        String comment) {
      value(new ConfigValue.MapList(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Declares one object with fields of any YAML type ({@link ConfigValue.MapValue}), read with
     * {@link ConfigView#getMap(String)}. When all fields have the same type, prefer
     * {@link #stringMapValue} or {@link #intMapValue}, which convert and check each value.
     *
     * @param key the key within this section
     * @param defaultValue the default value; use only String, Number, Boolean, List and Map
     *     values, or writing the file fails
     * @param comment the comment written after the key, or {@code null}
     * @return this builder
     */
    public SectionBuilder mapValue(
        String key,
        java.util.Map<String, Object> defaultValue,
        String comment) {
      value(new ConfigValue.MapValue(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Declares one object with fields of any YAML type ({@link ConfigValue.MapValue}) with a
     * validator, read with {@link ConfigView#getMap(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; use only String, Number, Boolean, List and Map
     *     values, or writing the file fails; must pass {@code validator}
     * @param validator checks the value
     * @param comment the comment written after the key, or {@code null}
     * @return this builder
     */
    public SectionBuilder mapValue(
        String key,
        java.util.Map<String, Object> defaultValue,
        Validator<java.util.Map<String, Object>> validator,
        String comment) {
      value(new ConfigValue.MapValue(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Declares a map of text keys to text values ({@link ConfigValue.StringMap}), read with
     * {@link ConfigView#getStringMap(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; pass {@code Map.of(...)}, or a
     *     {@code LinkedHashMap} to control the order in the file
     * @param comment the comment written after the key, or {@code null}
     * @return this builder
     */
    public SectionBuilder stringMapValue(
        String key,
        java.util.Map<String, String> defaultValue,
        String comment) {
      value(new ConfigValue.StringMap(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Declares a map of text keys to text values ({@link ConfigValue.StringMap}) with a
     * validator, read with {@link ConfigView#getStringMap(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; pass {@code Map.of(...)}, or a
     *     {@code LinkedHashMap} to control the order in the file; must pass {@code validator}
     * @param validator checks the value
     * @param comment the comment written after the key, or {@code null}
     * @return this builder
     */
    public SectionBuilder stringMapValue(
        String key,
        java.util.Map<String, String> defaultValue,
        Validator<java.util.Map<String, String>> validator,
        String comment) {
      value(new ConfigValue.StringMap(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Declares a map of text keys to whole numbers ({@link ConfigValue.IntMap}), read with
     * {@link ConfigView#getIntMap(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; pass {@code Map.of(...)}, or a
     *     {@code LinkedHashMap} to control the order in the file
     * @param comment the comment written after the key, or {@code null}
     * @return this builder
     */
    public SectionBuilder intMapValue(
        String key,
        java.util.Map<String, Integer> defaultValue,
        String comment) {
      value(new ConfigValue.IntMap(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Declares a map of text keys to whole numbers ({@link ConfigValue.IntMap}) with a
     * validator, read with {@link ConfigView#getIntMap(String)}.
     *
     * @param key the key within this section
     * @param defaultValue the default value; pass {@code Map.of(...)}, or a
     *     {@code LinkedHashMap} to control the order in the file; must pass {@code validator}
     * @param validator checks the value
     * @param comment the comment written after the key, or {@code null}
     * @return this builder
     */
    public SectionBuilder intMapValue(
        String key,
        java.util.Map<String, Integer> defaultValue,
        Validator<java.util.Map<String, Integer>> validator,
        String comment) {
      value(new ConfigValue.IntMap(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Starts a section nested inside this one: {@code section("game").section("rewards")}
     * declares values under {@code "game.rewards"}. Call {@link #parent()} on the nested
     * builder to continue with this one.
     *
     * @param sectionName the nested section's name, relative to this section
     * @return a builder for the nested section
     */
    public SectionBuilder section(String sectionName) {
      return new SectionBuilder(prefix + "." + sectionName, this);
    }

    /**
     * Returns the builder this one was nested from with {@link #section(String)}, so a chain
     * can continue at the outer level.
     *
     * @return the outer section's builder
     * @throws IllegalStateException if this builder came from {@link ConfigSchema#section(String)}
     *     and so has no outer section
     */
    public SectionBuilder parent() {
      if (parent == null) {
        throw new IllegalStateException("Cannot call parent() on a root section. This section has no parent.");
      }
      return parent;
    }
  }
}
