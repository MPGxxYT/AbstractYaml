package me.mortaldev.config2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Defines the structure of a configuration file.
 * A schema declares all config values and their defaults.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class AbilitiesSchema extends ConfigSchema {
 *     public AbilitiesSchema() {
 *         super("abilities");
 *
 *         value(new ConfigValue.Int("keraunos.cooldown", 10, "Cooldown in seconds"));
 *         value(new ConfigValue.Double("keraunos.radius", 3.0,
 *             Validator.min(0.0), "Effect radius in blocks"));
 *         value(new ConfigValue.StringList("keraunos.allowed-items",
 *             List.of("DIAMOND_SWORD", "IRON_SWORD"), "Items that can be used"));
 *     }
 * }
 * }</pre>
 */
public abstract class ConfigSchema {
  private final String name;
  private final Map<String, ConfigValue<?>> values;
  private String fileHeader;

  protected ConfigSchema(String name) {
    this.name = name;
    this.values = new LinkedHashMap<>();
  }

  /**
   * Returns the name of this configuration (used as filename without .yml extension).
   */
  public String name() {
    return name;
  }

  /**
   * Returns all config values defined in this schema.
   */
  public Set<ConfigValue<?>> values() {
    return Set.copyOf(values.values());
  }

  /**
   * Gets a config value by path.
   */
  public ConfigValue<?> getValue(String path) {
    return values.get(path);
  }

  /**
   * Checks if this schema contains a value at the given path.
   */
  public boolean hasValue(String path) {
    return values.containsKey(path);
  }

  /**
   * Returns the file header comment (optional).
   */
  public String fileHeader() {
    return fileHeader;
  }

  /**
   * Registers a config value in this schema.
   * Call this from your schema's constructor.
   */
  protected <T> void value(ConfigValue<T> configValue) {
    values.put(configValue.path(), configValue);
  }

  /**
   * Sets a file header comment that appears at the top of the YAML file.
   */
  protected void header(String header) {
    this.fileHeader = header;
  }

  /**
   * Creates a section builder for organizing related values.
   */
  protected SectionBuilder section(String prefix) {
    return new SectionBuilder(prefix);
  }

  /**
   * Helper class for defining config sections with a common prefix.
   */
  protected class SectionBuilder {
    private final String prefix;
    private String sectionHeader;
    private final SectionBuilder parent;

    private SectionBuilder(String prefix) {
      this(prefix, null);
    }

    private SectionBuilder(String prefix, SectionBuilder parent) {
      this.prefix = prefix;
      this.parent = parent;
    }

    /**
     * Sets a header comment for this section.
     */
    public SectionBuilder header(String header) {
      this.sectionHeader = header;
      return this;
    }

    /**
     * Adds an integer value to this section.
     */
    public SectionBuilder intValue(String key, int defaultValue, String comment) {
      value(new ConfigValue.Int(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Adds an integer value with validation to this section.
     */
    public SectionBuilder intValue(
        String key, int defaultValue, Validator<Integer> validator, String comment) {
      value(new ConfigValue.Int(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Adds a double value to this section.
     */
    public SectionBuilder doubleValue(String key, double defaultValue, String comment) {
      value(new ConfigValue.Double(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Adds a double value with validation to this section.
     */
    public SectionBuilder doubleValue(
        String key, double defaultValue, Validator<Double> validator, String comment) {
      value(new ConfigValue.Double(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Adds a string value to this section.
     */
    public SectionBuilder stringValue(String key, String defaultValue, String comment) {
      value(new ConfigValue.String(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Adds a string value with validation to this section.
     */
    public SectionBuilder stringValue(
        String key, String defaultValue, Validator<String> validator, String comment) {
      value(new ConfigValue.String(prefix + "." + key, defaultValue, validator, comment));
      return this;
    }

    /**
     * Adds a boolean value to this section.
     */
    public SectionBuilder boolValue(String key, boolean defaultValue, String comment) {
      value(new ConfigValue.Boolean(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Adds a string list to this section.
     */
    public SectionBuilder stringList(
        String key, java.util.List<String> defaultValue, String comment) {
      value(new ConfigValue.StringList(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Adds a string list with validation to this section.
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
     * Adds an integer list to this section.
     */
    public SectionBuilder intList(
        String key, java.util.List<Integer> defaultValue, String comment) {
      value(new ConfigValue.IntList(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Adds a double list to this section.
     */
    public SectionBuilder doubleList(
        String key, java.util.List<java.lang.Double> defaultValue, String comment) {
      value(new ConfigValue.DoubleList(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Adds an integer list with validation to this section.
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
     * Adds a double list with validation to this section.
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
     * Adds a string list value to this section.
     */
    public SectionBuilder stringListValue(
        String key, java.util.List<String> defaultValue, String comment) {
      value(new ConfigValue.StringList(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Adds a string list with validation to this section.
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
     * Adds a list of maps (objects) to this section.
     * Useful for complex configuration structures like lists of objects.
     */
    public SectionBuilder mapListValue(
        String key,
        java.util.List<java.util.Map<String, Object>> defaultValue,
        String comment) {
      value(new ConfigValue.MapList(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Adds a list of maps (objects) with validation to this section.
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
     * Adds a single map/object to this section.
     * Useful for configuration objects with mixed types.
     */
    public SectionBuilder mapValue(
        String key,
        java.util.Map<String, Object> defaultValue,
        String comment) {
      value(new ConfigValue.MapValue(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Adds a single map/object with validation to this section.
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
     * Adds a String-to-String map to this section.
     * Useful for simple key-value pairs.
     */
    public SectionBuilder stringMapValue(
        String key,
        java.util.Map<String, String> defaultValue,
        String comment) {
      value(new ConfigValue.StringMap(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Adds a String-to-String map with validation to this section.
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
     * Adds a String-to-Integer map to this section.
     * Useful for numeric mappings.
     */
    public SectionBuilder intMapValue(
        String key,
        java.util.Map<String, Integer> defaultValue,
        String comment) {
      value(new ConfigValue.IntMap(prefix + "." + key, defaultValue, comment));
      return this;
    }

    /**
     * Adds a String-to-Integer map with validation to this section.
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
     * Creates a nested section within this section.
     * The nested section's prefix will be this section's prefix + the new section name.
     */
    public SectionBuilder section(String sectionName) {
      return new SectionBuilder(prefix + "." + sectionName, this);
    }

    /**
     * Returns to the parent section builder.
     * This allows for chaining nested sections and returning to the parent level.
     * Returns the ConfigSchema's section builder if called on a root section.
     */
    public SectionBuilder parent() {
      if (parent == null) {
        throw new IllegalStateException("Cannot call parent() on a root section. This section has no parent.");
      }
      return parent;
    }
  }
}
