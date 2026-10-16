package me.mortaldev.abstractyaml.config2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Base interface for all configuration values.
 * Provides type-safe access to configuration data without casting.
 *
 * <p>Each value type owns the conversion from plain YAML data (String, Number, Boolean, List,
 * Map) to its own Java type through {@link #read(Object)}. Because the interface is sealed and
 * {@code read} is abstract, a new value type cannot compile until it says how it is read.
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
   * Creates a new ConfigValue holding {@code raw} converted to this value's type.
   *
   * <p>{@code raw} is plain YAML data as produced by a YAML parser: String, Number, Boolean,
   * List or Map. Conversion is lenient where no information is lost (a whole {@code 5.0} reads
   * as the int {@code 5}, a numeric string reads as a number) and strict otherwise.
   *
   * @throws IllegalArgumentException if {@code raw} cannot be converted to this type
   */
  ConfigValue<T> read(Object raw);

  /**
   * Validates the current value.
   */
  default ValidationResult validate() {
    return validator().validate(value());
  }

  // ===== Conversion from plain YAML data =====

  private static Integer toInt(Object raw) {
    if (raw instanceof java.lang.String s) {
      try {
        raw = new java.math.BigDecimal(s.trim());
      } catch (NumberFormatException e) {
        throw mismatch("an integer", raw);
      }
    }
    if (raw instanceof Integer i) {
      return i;
    }
    if (raw instanceof Number n) {
      double d = n.doubleValue();
      if (d != Math.rint(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
        throw mismatch("an integer", raw);
      }
      return n.intValue();
    }
    throw mismatch("an integer", raw);
  }

  private static java.lang.Double toDouble(Object raw) {
    if (raw instanceof Number n) {
      return n.doubleValue();
    }
    if (raw instanceof java.lang.String s) {
      try {
        return java.lang.Double.parseDouble(s.trim());
      } catch (NumberFormatException e) {
        throw mismatch("a number", raw);
      }
    }
    throw mismatch("a number", raw);
  }

  private static java.lang.String toStr(Object raw) {
    if (raw instanceof java.lang.String
        || raw instanceof Number
        || raw instanceof java.lang.Boolean
        || raw instanceof Character) {
      return raw.toString();
    }
    throw mismatch("a string", raw);
  }

  private static java.lang.Boolean toBool(Object raw) {
    if (raw instanceof java.lang.Boolean b) {
      return b;
    }
    if (raw instanceof java.lang.String s) {
      if (s.equalsIgnoreCase("true")) {
        return true;
      }
      if (s.equalsIgnoreCase("false")) {
        return false;
      }
    }
    throw mismatch("true or false", raw);
  }

  private static <E> List<E> toList(Object raw, Function<Object, E> element) {
    if (!(raw instanceof List<?> list)) {
      throw mismatch("a list", raw);
    }
    List<E> result = new ArrayList<>(list.size());
    for (int i = 0; i < list.size(); i++) {
      try {
        result.add(element.apply(list.get(i)));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("element " + i + ": " + e.getMessage());
      }
    }
    return Collections.unmodifiableList(result);
  }

  private static <V> Map<java.lang.String, V> toMap(Object raw, Function<Object, V> entry) {
    if (!(raw instanceof Map<?, ?> map)) {
      throw mismatch("a map", raw);
    }
    Map<java.lang.String, V> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> e : map.entrySet()) {
      java.lang.String key = java.lang.String.valueOf(e.getKey());
      try {
        result.put(key, entry.apply(e.getValue()));
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException("key '" + key + "': " + ex.getMessage());
      }
    }
    return Collections.unmodifiableMap(result);
  }

  /** Copies nested YAML data, normalizing map keys to strings. */
  private static Object toPlain(Object raw) {
    if (raw instanceof Map<?, ?>) {
      return toMap(raw, ConfigValue::toPlain);
    }
    if (raw instanceof List<?>) {
      return toList(raw, ConfigValue::toPlain);
    }
    return raw;
  }

  @SuppressWarnings("unchecked")
  private static Map<java.lang.String, Object> toPlainMap(Object raw) {
    return (Map<java.lang.String, Object>) toMap(raw, ConfigValue::toPlain);
  }

  private static IllegalArgumentException mismatch(java.lang.String expected, Object raw) {
    java.lang.String actual =
        raw == null ? "nothing" : raw.getClass().getSimpleName() + " " + describe(raw);
    return new IllegalArgumentException("expected " + expected + ", got " + actual);
  }

  private static java.lang.String describe(Object raw) {
    java.lang.String text = raw instanceof java.lang.String ? "\"" + raw + "\"" : raw.toString();
    return text.length() > 40 ? text.substring(0, 37) + "..." : text;
  }

  // ===== Concrete Implementations =====

  record String(
      java.lang.String path,
      java.lang.String value,
      java.lang.String defaultValue,
      java.lang.String comment,
      Validator<java.lang.String> validator) implements ConfigValue<java.lang.String> {

    public String {
      validator = validator != null ? validator : Validator.none();
    }

    public String(java.lang.String path, java.lang.String defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    public String(java.lang.String path, java.lang.String defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    public String(
        java.lang.String path,
        java.lang.String defaultValue,
        Validator<java.lang.String> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<java.lang.String> withValue(java.lang.String newValue) {
      return new String(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<java.lang.String> read(Object raw) {
      return withValue(toStr(raw));
    }
  }

  record Int(
      java.lang.String path,
      Integer value,
      Integer defaultValue,
      java.lang.String comment,
      Validator<Integer> validator) implements ConfigValue<Integer> {

    public Int {
      validator = validator != null ? validator : Validator.none();
    }

    public Int(java.lang.String path, int defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    public Int(java.lang.String path, int defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    public Int(
        java.lang.String path,
        int defaultValue,
        Validator<Integer> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<Integer> withValue(Integer newValue) {
      return new Int(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<Integer> read(Object raw) {
      return withValue(toInt(raw));
    }
  }

  record Double(
      java.lang.String path,
      java.lang.Double value,
      java.lang.Double defaultValue,
      java.lang.String comment,
      Validator<java.lang.Double> validator) implements ConfigValue<java.lang.Double> {

    public Double {
      validator = validator != null ? validator : Validator.none();
    }

    public Double(java.lang.String path, double defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    public Double(java.lang.String path, double defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    public Double(
        java.lang.String path,
        double defaultValue,
        Validator<java.lang.Double> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<java.lang.Double> withValue(java.lang.Double newValue) {
      return new Double(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<java.lang.Double> read(Object raw) {
      return withValue(toDouble(raw));
    }
  }

  record Boolean(
      java.lang.String path,
      java.lang.Boolean value,
      java.lang.Boolean defaultValue,
      java.lang.String comment,
      Validator<java.lang.Boolean> validator) implements ConfigValue<java.lang.Boolean> {

    public Boolean {
      validator = validator != null ? validator : Validator.none();
    }

    public Boolean(java.lang.String path, boolean defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    public Boolean(java.lang.String path, boolean defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    public Boolean(
        java.lang.String path,
        boolean defaultValue,
        Validator<java.lang.Boolean> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<java.lang.Boolean> withValue(java.lang.Boolean newValue) {
      return new Boolean(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<java.lang.Boolean> read(Object raw) {
      return withValue(toBool(raw));
    }
  }

  record StringList(
      java.lang.String path,
      List<java.lang.String> value,
      List<java.lang.String> defaultValue,
      java.lang.String comment,
      Validator<List<java.lang.String>> validator)
      implements ConfigValue<List<java.lang.String>> {

    public StringList {
      validator = validator != null ? validator : Validator.none();
    }

    public StringList(java.lang.String path, List<java.lang.String> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    public StringList(
        java.lang.String path, List<java.lang.String> defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    public StringList(
        java.lang.String path,
        List<java.lang.String> defaultValue,
        Validator<List<java.lang.String>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<List<java.lang.String>> withValue(List<java.lang.String> newValue) {
      return new StringList(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<List<java.lang.String>> read(Object raw) {
      return withValue(toList(raw, ConfigValue::toStr));
    }
  }

  record IntList(
      java.lang.String path,
      List<Integer> value,
      List<Integer> defaultValue,
      java.lang.String comment,
      Validator<List<Integer>> validator)
      implements ConfigValue<List<Integer>> {

    public IntList {
      validator = validator != null ? validator : Validator.none();
    }

    public IntList(java.lang.String path, List<Integer> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    public IntList(java.lang.String path, List<Integer> defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    public IntList(
        java.lang.String path,
        List<Integer> defaultValue,
        Validator<List<Integer>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<List<Integer>> withValue(List<Integer> newValue) {
      return new IntList(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<List<Integer>> read(Object raw) {
      return withValue(toList(raw, ConfigValue::toInt));
    }
  }

  record DoubleList(
      java.lang.String path,
      List<java.lang.Double> value,
      List<java.lang.Double> defaultValue,
      java.lang.String comment,
      Validator<List<java.lang.Double>> validator)
      implements ConfigValue<List<java.lang.Double>> {

    public DoubleList {
      validator = validator != null ? validator : Validator.none();
    }

    public DoubleList(java.lang.String path, List<java.lang.Double> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    public DoubleList(
        java.lang.String path, List<java.lang.Double> defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    public DoubleList(
        java.lang.String path,
        List<java.lang.Double> defaultValue,
        Validator<List<java.lang.Double>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<List<java.lang.Double>> withValue(List<java.lang.Double> newValue) {
      return new DoubleList(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<List<java.lang.Double>> read(Object raw) {
      return withValue(toList(raw, ConfigValue::toDouble));
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
      List<Map<java.lang.String, Object>> value,
      List<Map<java.lang.String, Object>> defaultValue,
      java.lang.String comment,
      Validator<List<Map<java.lang.String, Object>>> validator)
      implements ConfigValue<List<Map<java.lang.String, Object>>> {

    public MapList {
      validator = validator != null ? validator : Validator.none();
    }

    public MapList(java.lang.String path, List<Map<java.lang.String, Object>> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    public MapList(
        java.lang.String path,
        List<Map<java.lang.String, Object>> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    public MapList(
        java.lang.String path,
        List<Map<java.lang.String, Object>> defaultValue,
        Validator<List<Map<java.lang.String, Object>>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<List<Map<java.lang.String, Object>>> withValue(
        List<Map<java.lang.String, Object>> newValue) {
      return new MapList(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<List<Map<java.lang.String, Object>>> read(Object raw) {
      return withValue(toList(raw, ConfigValue::toPlainMap));
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
      Map<java.lang.String, Object> value,
      Map<java.lang.String, Object> defaultValue,
      java.lang.String comment,
      Validator<Map<java.lang.String, Object>> validator)
      implements ConfigValue<Map<java.lang.String, Object>> {

    public MapValue {
      validator = validator != null ? validator : Validator.none();
    }

    public MapValue(java.lang.String path, Map<java.lang.String, Object> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    public MapValue(
        java.lang.String path,
        Map<java.lang.String, Object> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    public MapValue(
        java.lang.String path,
        Map<java.lang.String, Object> defaultValue,
        Validator<Map<java.lang.String, Object>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<Map<java.lang.String, Object>> withValue(
        Map<java.lang.String, Object> newValue) {
      return new MapValue(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<Map<java.lang.String, Object>> read(Object raw) {
      return withValue(toPlainMap(raw));
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
      Map<java.lang.String, java.lang.String> value,
      Map<java.lang.String, java.lang.String> defaultValue,
      java.lang.String comment,
      Validator<Map<java.lang.String, java.lang.String>> validator)
      implements ConfigValue<Map<java.lang.String, java.lang.String>> {

    public StringMap {
      validator = validator != null ? validator : Validator.none();
    }

    public StringMap(java.lang.String path, Map<java.lang.String, java.lang.String> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    public StringMap(
        java.lang.String path,
        Map<java.lang.String, java.lang.String> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    public StringMap(
        java.lang.String path,
        Map<java.lang.String, java.lang.String> defaultValue,
        Validator<Map<java.lang.String, java.lang.String>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<Map<java.lang.String, java.lang.String>> withValue(
        Map<java.lang.String, java.lang.String> newValue) {
      return new StringMap(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<Map<java.lang.String, java.lang.String>> read(Object raw) {
      return withValue(toMap(raw, ConfigValue::toStr));
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
      Map<java.lang.String, Integer> value,
      Map<java.lang.String, Integer> defaultValue,
      java.lang.String comment,
      Validator<Map<java.lang.String, Integer>> validator)
      implements ConfigValue<Map<java.lang.String, Integer>> {

    public IntMap {
      validator = validator != null ? validator : Validator.none();
    }

    public IntMap(java.lang.String path, Map<java.lang.String, Integer> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    public IntMap(
        java.lang.String path,
        Map<java.lang.String, Integer> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    public IntMap(
        java.lang.String path,
        Map<java.lang.String, Integer> defaultValue,
        Validator<Map<java.lang.String, Integer>> validator,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<Map<java.lang.String, Integer>> withValue(
        Map<java.lang.String, Integer> newValue) {
      return new IntMap(path, newValue, defaultValue, comment, validator);
    }

    @Override
    public ConfigValue<Map<java.lang.String, Integer>> read(Object raw) {
      return withValue(toMap(raw, ConfigValue::toInt));
    }
  }
}
