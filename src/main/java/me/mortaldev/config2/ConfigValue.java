package me.mortaldev.config2;

/**
 * Base interface for all configuration values.
 * Provides type-safe access to configuration data without casting.
 *
 * @param <T> the type of the configuration value
 */
public sealed interface ConfigValue<T> permits
    ConfigValue.String,
    ConfigValue.Int,
    ConfigValue.Double,
    ConfigValue.Boolean,
    ConfigValue.StringList,
    ConfigValue.IntList,
    ConfigValue.DoubleList,
    ConfigValue.MapList,
    ConfigValue.MapValue,
    ConfigValue.StringMap,
    ConfigValue.IntMap {

  /**
   * The configuration path for this value (e.g., "keraunos.cooldown").
   */
  java.lang.String path();

  /**
   * The current value.
   */
  T value();

  /**
   * The default value used when the config is missing or invalid.
   */
  T defaultValue();

  /**
   * Optional comment to display in the generated YAML file.
   */
  java.lang.String comment();

  /**
   * Validator for this config value.
   */
  Validator<T> validator();

  /**
   * Creates a new ConfigValue with a different value.
   * ConfigValues are immutable, so this returns a new instance.
   */
  ConfigValue<T> withValue(T newValue);

  /**
   * Validates the current value.
   */
  ValidationResult validate();

  // ===== Concrete Implementations =====

  record String(
      java.lang.String path,
      java.lang.String value,
      java.lang.String defaultValue,
      java.lang.String comment,
      Validator<java.lang.String> validator) implements ConfigValue<java.lang.String> {

    public String(java.lang.String path, java.lang.String defaultValue) {
      this(path, defaultValue, defaultValue, null, Validator.none());
    }

    public String(java.lang.String path, java.lang.String defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, Validator.none());
    }

    public String(
        java.lang.String path,
        java.lang.String defaultValue,
        Validator<java.lang.String> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator != null ? validator : Validator.none());
    }

    @Override
    public ConfigValue<java.lang.String> withValue(java.lang.String newValue) {
      return new String(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ValidationResult validate() {
      return validator.validate(value);
    }
  }

  record Int(
      java.lang.String path,
      Integer value,
      Integer defaultValue,
      java.lang.String comment,
      Validator<Integer> validator) implements ConfigValue<Integer> {

    public Int(java.lang.String path, int defaultValue) {
      this(path, defaultValue, defaultValue, null, Validator.none());
    }

    public Int(java.lang.String path, int defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, Validator.none());
    }

    public Int(
        java.lang.String path,
        int defaultValue,
        Validator<Integer> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator != null ? validator : Validator.none());
    }

    @Override
    public ConfigValue<Integer> withValue(Integer newValue) {
      return new Int(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ValidationResult validate() {
      return validator.validate(value);
    }
  }

  record Double(
      java.lang.String path,
      java.lang.Double value,
      java.lang.Double defaultValue,
      java.lang.String comment,
      Validator<java.lang.Double> validator) implements ConfigValue<java.lang.Double> {

    public Double(java.lang.String path, double defaultValue) {
      this(path, defaultValue, defaultValue, null, Validator.none());
    }

    public Double(java.lang.String path, double defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, Validator.none());
    }

    public Double(
        java.lang.String path,
        double defaultValue,
        Validator<java.lang.Double> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator != null ? validator : Validator.none());
    }

    @Override
    public ConfigValue<java.lang.Double> withValue(java.lang.Double newValue) {
      return new Double(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ValidationResult validate() {
      return validator.validate(value);
    }
  }

  record Boolean(
      java.lang.String path,
      java.lang.Boolean value,
      java.lang.Boolean defaultValue,
      java.lang.String comment,
      Validator<java.lang.Boolean> validator) implements ConfigValue<java.lang.Boolean> {

    public Boolean(java.lang.String path, boolean defaultValue) {
      this(path, defaultValue, defaultValue, null, Validator.none());
    }

    public Boolean(java.lang.String path, boolean defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, Validator.none());
    }

    public Boolean(
        java.lang.String path,
        boolean defaultValue,
        Validator<java.lang.Boolean> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator != null ? validator : Validator.none());
    }

    @Override
    public ConfigValue<java.lang.Boolean> withValue(java.lang.Boolean newValue) {
      return new Boolean(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ValidationResult validate() {
      return validator.validate(value);
    }
  }

  record StringList(
      java.lang.String path,
      java.util.List<java.lang.String> value,
      java.util.List<java.lang.String> defaultValue,
      java.lang.String comment,
      Validator<java.util.List<java.lang.String>> validator)
      implements ConfigValue<java.util.List<java.lang.String>> {

    public StringList(java.lang.String path, java.util.List<java.lang.String> defaultValue) {
      this(path, defaultValue, defaultValue, null, Validator.none());
    }

    public StringList(
        java.lang.String path,
        java.util.List<java.lang.String> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, Validator.none());
    }

    public StringList(
        java.lang.String path,
        java.util.List<java.lang.String> defaultValue,
        Validator<java.util.List<java.lang.String>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator != null ? validator : Validator.none());
    }

    @Override
    public ConfigValue<java.util.List<java.lang.String>> withValue(
        java.util.List<java.lang.String> newValue) {
      return new StringList(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ValidationResult validate() {
      return validator.validate(value);
    }
  }

  record IntList(
      java.lang.String path,
      java.util.List<Integer> value,
      java.util.List<Integer> defaultValue,
      java.lang.String comment,
      Validator<java.util.List<Integer>> validator)
      implements ConfigValue<java.util.List<Integer>> {

    public IntList(java.lang.String path, java.util.List<Integer> defaultValue) {
      this(path, defaultValue, defaultValue, null, Validator.none());
    }

    public IntList(
        java.lang.String path, java.util.List<Integer> defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, Validator.none());
    }

    public IntList(
        java.lang.String path,
        java.util.List<Integer> defaultValue,
        Validator<java.util.List<Integer>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator != null ? validator : Validator.none());
    }

    @Override
    public ConfigValue<java.util.List<Integer>> withValue(java.util.List<Integer> newValue) {
      return new IntList(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ValidationResult validate() {
      return validator.validate(value);
    }
  }

  record DoubleList(
      java.lang.String path,
      java.util.List<java.lang.Double> value,
      java.util.List<java.lang.Double> defaultValue,
      java.lang.String comment,
      Validator<java.util.List<java.lang.Double>> validator)
      implements ConfigValue<java.util.List<java.lang.Double>> {

    public DoubleList(java.lang.String path, java.util.List<java.lang.Double> defaultValue) {
      this(path, defaultValue, defaultValue, null, Validator.none());
    }

    public DoubleList(
        java.lang.String path,
        java.util.List<java.lang.Double> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, Validator.none());
    }

    public DoubleList(
        java.lang.String path,
        java.util.List<java.lang.Double> defaultValue,
        Validator<java.util.List<java.lang.Double>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator != null ? validator : Validator.none());
    }

    @Override
    public ConfigValue<java.util.List<java.lang.Double>> withValue(
        java.util.List<java.lang.Double> newValue) {
      return new DoubleList(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ValidationResult validate() {
      return validator.validate(value);
    }
  }

  /**
   * List of maps (objects) - useful for complex configuration structures.
   * Example YAML:
   * <pre>
   * points:
   *   - name: "point1"
   *     radius: 3.0
   *     duration: 100
   *   - name: "point2"
   *     radius: 5.0
   *     duration: 200
   * </pre>
   */
  record MapList(
      java.lang.String path,
      java.util.List<java.util.Map<java.lang.String, Object>> value,
      java.util.List<java.util.Map<java.lang.String, Object>> defaultValue,
      java.lang.String comment,
      Validator<java.util.List<java.util.Map<java.lang.String, Object>>> validator)
      implements ConfigValue<java.util.List<java.util.Map<java.lang.String, Object>>> {

    public MapList(
        java.lang.String path,
        java.util.List<java.util.Map<java.lang.String, Object>> defaultValue) {
      this(path, defaultValue, defaultValue, null, Validator.none());
    }

    public MapList(
        java.lang.String path,
        java.util.List<java.util.Map<java.lang.String, Object>> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, Validator.none());
    }

    public MapList(
        java.lang.String path,
        java.util.List<java.util.Map<java.lang.String, Object>> defaultValue,
        Validator<java.util.List<java.util.Map<java.lang.String, Object>>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator != null ? validator : Validator.none());
    }

    @Override
    public ConfigValue<java.util.List<java.util.Map<java.lang.String, Object>>> withValue(
        java.util.List<java.util.Map<java.lang.String, Object>> newValue) {
      return new MapList(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ValidationResult validate() {
      return validator.validate(value);
    }
  }

  /**
   * Single map/object - useful for configuration objects.
   * Example YAML:
   * <pre>
   * player-stats:
   *   health: 20
   *   mana: 100
   *   level: 5
   * </pre>
   */
  record MapValue(
      java.lang.String path,
      java.util.Map<java.lang.String, Object> value,
      java.util.Map<java.lang.String, Object> defaultValue,
      java.lang.String comment,
      Validator<java.util.Map<java.lang.String, Object>> validator)
      implements ConfigValue<java.util.Map<java.lang.String, Object>> {

    public MapValue(
        java.lang.String path,
        java.util.Map<java.lang.String, Object> defaultValue) {
      this(path, defaultValue, defaultValue, null, Validator.none());
    }

    public MapValue(
        java.lang.String path,
        java.util.Map<java.lang.String, Object> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, Validator.none());
    }

    public MapValue(
        java.lang.String path,
        java.util.Map<java.lang.String, Object> defaultValue,
        Validator<java.util.Map<java.lang.String, Object>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator != null ? validator : Validator.none());
    }

    @Override
    public ConfigValue<java.util.Map<java.lang.String, Object>> withValue(
        java.util.Map<java.lang.String, Object> newValue) {
      return new MapValue(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ValidationResult validate() {
      return validator.validate(value);
    }
  }

  /**
   * String-to-String map - useful for simple key-value configurations.
   * Example YAML:
   * <pre>
   * team-colors:
   *   red: "#FF0000"
   *   blue: "#0000FF"
   *   green: "#00FF00"
   * </pre>
   */
  record StringMap(
      java.lang.String path,
      java.util.Map<java.lang.String, java.lang.String> value,
      java.util.Map<java.lang.String, java.lang.String> defaultValue,
      java.lang.String comment,
      Validator<java.util.Map<java.lang.String, java.lang.String>> validator)
      implements ConfigValue<java.util.Map<java.lang.String, java.lang.String>> {

    public StringMap(
        java.lang.String path,
        java.util.Map<java.lang.String, java.lang.String> defaultValue) {
      this(path, defaultValue, defaultValue, null, Validator.none());
    }

    public StringMap(
        java.lang.String path,
        java.util.Map<java.lang.String, java.lang.String> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, Validator.none());
    }

    public StringMap(
        java.lang.String path,
        java.util.Map<java.lang.String, java.lang.String> defaultValue,
        Validator<java.util.Map<java.lang.String, java.lang.String>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator != null ? validator : Validator.none());
    }

    @Override
    public ConfigValue<java.util.Map<java.lang.String, java.lang.String>> withValue(
        java.util.Map<java.lang.String, java.lang.String> newValue) {
      return new StringMap(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ValidationResult validate() {
      return validator.validate(value);
    }
  }

  /**
   * String-to-Integer map - useful for numeric mappings.
   * Example YAML:
   * <pre>
   * class-levels:
   *   warrior: 10
   *   mage: 8
   *   archer: 12
   * </pre>
   */
  record IntMap(
      java.lang.String path,
      java.util.Map<java.lang.String, Integer> value,
      java.util.Map<java.lang.String, Integer> defaultValue,
      java.lang.String comment,
      Validator<java.util.Map<java.lang.String, Integer>> validator)
      implements ConfigValue<java.util.Map<java.lang.String, Integer>> {

    public IntMap(
        java.lang.String path,
        java.util.Map<java.lang.String, Integer> defaultValue) {
      this(path, defaultValue, defaultValue, null, Validator.none());
    }

    public IntMap(
        java.lang.String path,
        java.util.Map<java.lang.String, Integer> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, Validator.none());
    }

    public IntMap(
        java.lang.String path,
        java.util.Map<java.lang.String, Integer> defaultValue,
        Validator<java.util.Map<java.lang.String, Integer>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator != null ? validator : Validator.none());
    }

    @Override
    public ConfigValue<java.util.Map<java.lang.String, Integer>> withValue(
        java.util.Map<java.lang.String, Integer> newValue) {
      return new IntMap(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ValidationResult validate() {
      return validator.validate(value);
    }
  }
}