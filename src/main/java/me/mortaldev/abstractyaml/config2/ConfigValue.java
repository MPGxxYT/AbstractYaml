package me.mortaldev.abstractyaml.config2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * One value in a config file: its path, its current value, its default, an optional comment for
 * the generated file, and a validator.
 *
 * <p>You declare values in a {@link ConfigSchema}, either directly with
 * {@code value(new ConfigValue.Int("fireball.cooldown", 10, "Seconds between casts"))} or through
 * {@link ConfigSchema.SectionBuilder} methods such as {@code intValue(...)}. You read them back
 * through the typed getters of {@link ConfigView} ({@code getInt}, {@code getStringList}, ...).
 *
 * <p>Every value is an immutable record. Changing a value ({@link #withValue}, {@link #read})
 * returns a new instance; the original is never modified.
 *
 * <h2>Types</h2>
 *
 * <table>
 *   <caption>Value types, their Java type and the getter that reads them</caption>
 *   <tr><th>Record</th><th>Java type</th><th>Getter</th></tr>
 *   <tr><td>{@link String}</td><td>{@code java.lang.String}</td><td>{@code getString}</td></tr>
 *   <tr><td>{@link Int}</td><td>{@code Integer}</td><td>{@code getInt}</td></tr>
 *   <tr><td>{@link Double}</td><td>{@code java.lang.Double}</td><td>{@code getDouble}</td></tr>
 *   <tr><td>{@link Boolean}</td><td>{@code java.lang.Boolean}</td><td>{@code getBoolean}</td></tr>
 *   <tr><td>{@link StringList}</td><td>{@code List<String>}</td><td>{@code getStringList}</td></tr>
 *   <tr><td>{@link IntList}</td><td>{@code List<Integer>}</td><td>{@code getIntList}</td></tr>
 *   <tr><td>{@link DoubleList}</td><td>{@code List<Double>}</td><td>{@code getDoubleList}</td></tr>
 *   <tr><td>{@link MapList}</td><td>{@code List<Map<String, Object>>}</td>
 *       <td>{@code getMapList}</td></tr>
 *   <tr><td>{@link MapValue}</td><td>{@code Map<String, Object>}</td><td>{@code getMap}</td></tr>
 *   <tr><td>{@link StringMap}</td><td>{@code Map<String, String>}</td>
 *       <td>{@code getStringMap}</td></tr>
 *   <tr><td>{@link IntMap}</td><td>{@code Map<String, Integer>}</td><td>{@code getIntMap}</td></tr>
 * </table>
 *
 * <p>Inside this file the record {@code String} hides {@code java.lang.String}, which is why the
 * signatures here spell out {@code java.lang.String}. Your own code is not affected: it refers to
 * the record as {@code ConfigValue.String}.
 *
 * <h2>Paths</h2>
 *
 * <p>A path is the value's key in the file, with dots between nesting levels:
 * {@code "fireball.damage.base"} is the {@code base} key inside {@code damage} inside
 * {@code fireball}. A path cannot be both a value and a section; for example, declaring both
 * {@code "fireball"} and {@code "fireball.cooldown"} makes rendering the file throw
 * {@link IllegalStateException}.
 *
 * <h2>Reading from the file</h2>
 *
 * <p>When a config loads, each value converts the YAML data at its path with {@link #read}. The
 * conversion accepts anything it can convert without losing information and rejects the rest:
 * <ul>
 *   <li>Integers accept whole numbers in the {@code int} range, including {@code 5.0} and the
 *       text {@code "5"}. {@code 10.7} is rejected.
 *   <li>Doubles accept any number and numeric text such as {@code "2.5"}.
 *   <li>Booleans accept {@code true} and {@code false}, and the text {@code "true"} or
 *       {@code "false"} in any letter case.
 *   <li>Strings accept text, numbers and booleans ({@code 5} reads as {@code "5"}).
 *   <li>Lists and maps convert each element with the rules above.
 * </ul>
 *
 * <p>If the data cannot be converted, {@link ConfigLoader} logs a warning and keeps the default
 * for that value; the file is not changed. If the data converts but the {@link #validator()}
 * rejects it, loading fails with {@link ConfigLoader.ConfigLoadException}.
 *
 * <h2>Defaults</h2>
 *
 * <p>Lists and maps given as defaults are stored as they are, not copied. Pass an unmodifiable
 * collection ({@code List.of(...)}, {@code Map.of(...)}) so the default cannot change after the
 * schema is built. Values read from a file are always unmodifiable.
 *
 * @param <T> the Java type of the value, such as {@code Integer} or {@code List<String>}
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
   * Returns the full dotted path of this value in the file, such as
   * {@code "fireball.cooldown"}.
   *
   * @return the path, never {@code null}
   */
  java.lang.String path();

  /**
   * Returns the current value: the value read from the file, or the default if the file had no
   * usable value at this path.
   *
   * @return the current value
   */
  T value();

  /**
   * Returns the default value given when the schema declared this value. It is used when the
   * file is created, when the path is missing from the file, and when the data at the path
   * cannot be converted to this type.
   *
   * @return the default value
   */
  T defaultValue();

  /**
   * Returns the comment written after this value in the generated file, such as
   * {@code cooldown: 10  # Seconds between casts}.
   *
   * @return the comment, or {@code null} if the value has none
   */
  java.lang.String comment();

  /**
   * Returns the validator that checks this value on load and on {@link Config#with}.
   *
   * @return the validator; {@link Validator#none()} if none was given, never {@code null}
   */
  Validator<T> validator();

  /**
   * Returns a copy of this value with {@code newValue} as its value. The path, default, comment and
   * validator are kept. The new value is neither converted nor validated; use {@link #read} to
   * convert and {@link #validate()} to check it.
   *
   * @param newValue the new current value
   * @return a new instance; this one is unchanged
   */
  ConfigValue<T> withValue(T newValue);

  /**
   * Returns a copy of this value with {@code raw}, converted to this value's type, as its value.
   *
   * <p>{@code raw} is plain YAML data as produced by a YAML parser: String, Number, Boolean,
   * List or Map. Conversion is lenient where no information is lost (a whole {@code 5.0} reads
   * as the int {@code 5}, a numeric string reads as a number) and strict otherwise. See
   * "Reading from the file" above for the rules of each type. The result is not validated.
   *
   * @param raw the YAML data to convert
   * @return a new instance with the converted value; this one is unchanged
   * @throws IllegalArgumentException if {@code raw} cannot be converted to this type. The
   *     message says what was expected and what was found, and for lists and maps which element
   *     or key failed.
   */
  ConfigValue<T> read(Object raw);

  /**
   * Checks the current value with this value's {@link #validator()}.
   *
   * @return the result; {@link ValidationResult#isValid()} is {@code true} if the value passes
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

  /**
   * A text value, read with {@link ConfigView#getString(java.lang.String)}.
   *
   * <p>Reads YAML text, and also numbers and booleans, which become their text form
   * ({@code 5} reads as {@code "5"}, {@code true} as {@code "true"}). Lists and maps are
   * rejected. The value is always written to the file in double quotes.
   *
   * <pre>
   * prefix: "[Server] "
   * </pre>
   *
   * @param path the full dotted path, such as {@code "chat.prefix"}
   * @param value the current value
   * @param defaultValue the value used when the file has no usable value at {@code path}
   * @param comment the comment written after the value in the file, or {@code null} for none
   * @param validator checks the value; {@code null} means {@link Validator#none()}
   */
  record String(
      java.lang.String path,
      java.lang.String value,
      java.lang.String defaultValue,
      java.lang.String comment,
      Validator<java.lang.String> validator) implements ConfigValue<java.lang.String> {

    /**
     * Creates a text value with every field given. Most code uses the shorter constructors,
     * which set {@code value} to {@code defaultValue}.
     *
     * @param path the full dotted path
     * @param value the current value
     * @param defaultValue the default value
     * @param comment the file comment, or {@code null} for none
     * @param validator the validator, or {@code null} for {@link Validator#none()}
     */
    public String {
      validator = validator != null ? validator : Validator.none();
    }

    /**
     * Creates a text value with no comment and no validator.
     *
     * @param path the full dotted path, such as {@code "chat.prefix"}
     * @param defaultValue the default, also used as the starting value
     */
    public String(java.lang.String path, java.lang.String defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    /**
     * Creates a text value with a comment and no validator.
     *
     * @param path the full dotted path, such as {@code "chat.prefix"}
     * @param defaultValue the default, also used as the starting value
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
    public String(java.lang.String path, java.lang.String defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    /**
     * Creates a text value with a validator and a comment.
     *
     * @param path the full dotted path, such as {@code "chat.prefix"}
     * @param defaultValue the default, also used as the starting value. It must pass
     *     {@code validator}, or loading fails.
     * @param validator checks the value, for example {@link Validator#notEmpty()}
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
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

  /**
   * A whole number in the {@code int} range, read with {@link ConfigView#getInt(java.lang.String)}.
   *
   * <p>Reads whole numbers, including {@code 5.0} and the text {@code "5"}. A number with a
   * fraction ({@code 10.7}) or outside the {@code int} range is rejected, not rounded.
   *
   * <pre>
   * cooldown: 10
   * </pre>
   *
   * @param path the full dotted path, such as {@code "fireball.cooldown"}
   * @param value the current value
   * @param defaultValue the value used when the file has no usable value at {@code path}
   * @param comment the comment written after the value in the file, or {@code null} for none
   * @param validator checks the value; {@code null} means {@link Validator#none()}
   */
  record Int(
      java.lang.String path,
      Integer value,
      Integer defaultValue,
      java.lang.String comment,
      Validator<Integer> validator) implements ConfigValue<Integer> {

    /**
     * Creates an integer value with every field given. Most code uses the shorter constructors,
     * which set {@code value} to {@code defaultValue}.
     *
     * @param path the full dotted path
     * @param value the current value
     * @param defaultValue the default value
     * @param comment the file comment, or {@code null} for none
     * @param validator the validator, or {@code null} for {@link Validator#none()}
     */
    public Int {
      validator = validator != null ? validator : Validator.none();
    }

    /**
     * Creates an integer value with no comment and no validator.
     *
     * @param path the full dotted path, such as {@code "fireball.cooldown"}
     * @param defaultValue the default, also used as the starting value
     */
    public Int(java.lang.String path, int defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    /**
     * Creates an integer value with a comment and no validator.
     *
     * @param path the full dotted path, such as {@code "fireball.cooldown"}
     * @param defaultValue the default, also used as the starting value
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
    public Int(java.lang.String path, int defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    /**
     * Creates an integer value with a validator and a comment.
     *
     * @param path the full dotted path, such as {@code "fireball.cooldown"}
     * @param defaultValue the default, also used as the starting value. It must pass
     *     {@code validator}, or loading fails.
     * @param validator checks the value, for example {@code Validator.range(1, 300)}
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
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

  /**
   * A decimal number, read with {@link ConfigView#getDouble(java.lang.String)}.
   *
   * <p>Reads any number ({@code 3}, {@code 3.5}) and numeric text ({@code "3.5"}). Text is parsed
   * with {@link java.lang.Double#parseDouble(java.lang.String)}, so {@code "NaN"} and
   * {@code "Infinity"} are accepted too; add a range validator to rule them out.
   *
   * <pre>
   * radius: 3.0
   * </pre>
   *
   * @param path the full dotted path, such as {@code "fireball.radius"}
   * @param value the current value
   * @param defaultValue the value used when the file has no usable value at {@code path}
   * @param comment the comment written after the value in the file, or {@code null} for none
   * @param validator checks the value; {@code null} means {@link Validator#none()}
   */
  record Double(
      java.lang.String path,
      java.lang.Double value,
      java.lang.Double defaultValue,
      java.lang.String comment,
      Validator<java.lang.Double> validator) implements ConfigValue<java.lang.Double> {

    /**
     * Creates a decimal value with every field given. Most code uses the shorter constructors,
     * which set {@code value} to {@code defaultValue}.
     *
     * @param path the full dotted path
     * @param value the current value
     * @param defaultValue the default value
     * @param comment the file comment, or {@code null} for none
     * @param validator the validator, or {@code null} for {@link Validator#none()}
     */
    public Double {
      validator = validator != null ? validator : Validator.none();
    }

    /**
     * Creates a decimal value with no comment and no validator.
     *
     * @param path the full dotted path, such as {@code "fireball.radius"}
     * @param defaultValue the default, also used as the starting value
     */
    public Double(java.lang.String path, double defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    /**
     * Creates a decimal value with a comment and no validator.
     *
     * @param path the full dotted path, such as {@code "fireball.radius"}
     * @param defaultValue the default, also used as the starting value
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
    public Double(java.lang.String path, double defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    /**
     * Creates a decimal value with a validator and a comment.
     *
     * @param path the full dotted path, such as {@code "fireball.radius"}
     * @param defaultValue the default, also used as the starting value. It must pass
     *     {@code validator}, or loading fails.
     * @param validator checks the value, for example {@code Validator.min(0.0)}. Use double
     *     literals: {@code Validator.min(0)} picks the integer overload and does not compile here.
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
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

  /**
   * A true/false value, read with {@link ConfigView#getBoolean(java.lang.String)}.
   *
   * <p>Reads {@code true} and {@code false}, and the text {@code "true"} or {@code "false"} in
   * any letter case. Anything else, including {@code 1} and {@code 0}, is rejected.
   *
   * <pre>
   * enabled: true
   * </pre>
   *
   * @param path the full dotted path, such as {@code "fireball.enabled"}
   * @param value the current value
   * @param defaultValue the value used when the file has no usable value at {@code path}
   * @param comment the comment written after the value in the file, or {@code null} for none
   * @param validator checks the value; {@code null} means {@link Validator#none()}
   */
  record Boolean(
      java.lang.String path,
      java.lang.Boolean value,
      java.lang.Boolean defaultValue,
      java.lang.String comment,
      Validator<java.lang.Boolean> validator) implements ConfigValue<java.lang.Boolean> {

    /**
     * Creates a true/false value with every field given. Most code uses the shorter
     * constructors, which set {@code value} to {@code defaultValue}.
     *
     * @param path the full dotted path
     * @param value the current value
     * @param defaultValue the default value
     * @param comment the file comment, or {@code null} for none
     * @param validator the validator, or {@code null} for {@link Validator#none()}
     */
    public Boolean {
      validator = validator != null ? validator : Validator.none();
    }

    /**
     * Creates a true/false value with no comment and no validator.
     *
     * @param path the full dotted path, such as {@code "fireball.enabled"}
     * @param defaultValue the default, also used as the starting value
     */
    public Boolean(java.lang.String path, boolean defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    /**
     * Creates a true/false value with a comment and no validator.
     *
     * @param path the full dotted path, such as {@code "fireball.enabled"}
     * @param defaultValue the default, also used as the starting value
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
    public Boolean(java.lang.String path, boolean defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    /**
     * Creates a true/false value with a validator and a comment.
     *
     * @param path the full dotted path, such as {@code "fireball.enabled"}
     * @param defaultValue the default, also used as the starting value. It must pass
     *     {@code validator}, or loading fails.
     * @param validator checks the value
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
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

  /**
   * A list of text values, read with {@link ConfigView#getStringList(java.lang.String)}.
   *
   * <p>Reads a YAML list; each element follows the {@link String} rules, so numbers and booleans
   * become text. A single value that is not a list is rejected, as is a list containing a list
   * or map. The list read from the file is unmodifiable.
   *
   * <pre>
   * allowed-items:
   *   - "DIAMOND_SWORD"
   *   - "IRON_SWORD"
   * </pre>
   *
   * @param path the full dotted path, such as {@code "fireball.allowed-items"}
   * @param value the current value
   * @param defaultValue the value used when the file has no usable value at {@code path}
   * @param comment the comment written after the value in the file, or {@code null} for none
   * @param validator checks the value; {@code null} means {@link Validator#none()}
   */
  record StringList(
      java.lang.String path,
      List<java.lang.String> value,
      List<java.lang.String> defaultValue,
      java.lang.String comment,
      Validator<List<java.lang.String>> validator)
      implements ConfigValue<List<java.lang.String>> {

    /**
     * Creates a text list value with every field given. Most code uses the shorter
     * constructors, which set {@code value} to {@code defaultValue}.
     *
     * @param path the full dotted path
     * @param value the current value
     * @param defaultValue the default value
     * @param comment the file comment, or {@code null} for none
     * @param validator the validator, or {@code null} for {@link Validator#none()}
     */
    public StringList {
      validator = validator != null ? validator : Validator.none();
    }

    /**
     * Creates a text list value with no comment and no validator.
     *
     * @param path the full dotted path, such as {@code "fireball.allowed-items"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code List.of(...)}.
     */
    public StringList(java.lang.String path, List<java.lang.String> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    /**
     * Creates a text list value with a comment and no validator.
     *
     * @param path the full dotted path, such as {@code "fireball.allowed-items"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code List.of(...)}.
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
    public StringList(
        java.lang.String path, List<java.lang.String> defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    /**
     * Creates a text list value with a validator and a comment.
     *
     * @param path the full dotted path, such as {@code "fireball.allowed-items"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code List.of(...)}. It must pass {@code validator}, or loading fails.
     * @param validator checks the value, for example {@link Validator#allStringsNotEmpty()}
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
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

  /**
   * A list of whole numbers, read with {@link ConfigView#getIntList(java.lang.String)}.
   *
   * <p>Reads a YAML list; each element follows the {@link Int} rules. One bad element rejects
   * the list, and the error names its index. The list read from the file is unmodifiable.
   *
   * <pre>
   * levels: [1, 5, 10]
   * </pre>
   *
   * @param path the full dotted path, such as {@code "rewards.levels"}
   * @param value the current value
   * @param defaultValue the value used when the file has no usable value at {@code path}
   * @param comment the comment written after the value in the file, or {@code null} for none
   * @param validator checks the value; {@code null} means {@link Validator#none()}
   */
  record IntList(
      java.lang.String path,
      List<Integer> value,
      List<Integer> defaultValue,
      java.lang.String comment,
      Validator<List<Integer>> validator)
      implements ConfigValue<List<Integer>> {

    /**
     * Creates an integer list value with every field given. Most code uses the shorter
     * constructors, which set {@code value} to {@code defaultValue}.
     *
     * @param path the full dotted path
     * @param value the current value
     * @param defaultValue the default value
     * @param comment the file comment, or {@code null} for none
     * @param validator the validator, or {@code null} for {@link Validator#none()}
     */
    public IntList {
      validator = validator != null ? validator : Validator.none();
    }

    /**
     * Creates an integer list value with no comment and no validator.
     *
     * @param path the full dotted path, such as {@code "rewards.levels"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code List.of(...)}.
     */
    public IntList(java.lang.String path, List<Integer> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    /**
     * Creates an integer list value with a comment and no validator.
     *
     * @param path the full dotted path, such as {@code "rewards.levels"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code List.of(...)}.
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
    public IntList(java.lang.String path, List<Integer> defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    /**
     * Creates an integer list value with a validator and a comment.
     *
     * @param path the full dotted path, such as {@code "rewards.levels"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code List.of(...)}. It must pass {@code validator}, or loading fails.
     * @param validator checks the value, for example {@code Validator.allIntegersInRange(1, 100)}
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
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

  /**
   * A list of decimal numbers, read with {@link ConfigView#getDoubleList(java.lang.String)}.
   *
   * <p>Reads a YAML list; each element follows the {@link Double} rules. One bad element rejects
   * the list, and the error names its index. The list read from the file is unmodifiable.
   *
   * <pre>
   * multipliers: [1.0, 1.5, 2.0]
   * </pre>
   *
   * @param path the full dotted path, such as {@code "rewards.multipliers"}
   * @param value the current value
   * @param defaultValue the value used when the file has no usable value at {@code path}
   * @param comment the comment written after the value in the file, or {@code null} for none
   * @param validator checks the value; {@code null} means {@link Validator#none()}
   */
  record DoubleList(
      java.lang.String path,
      List<java.lang.Double> value,
      List<java.lang.Double> defaultValue,
      java.lang.String comment,
      Validator<List<java.lang.Double>> validator)
      implements ConfigValue<List<java.lang.Double>> {

    /**
     * Creates a decimal list value with every field given. Most code uses the shorter
     * constructors, which set {@code value} to {@code defaultValue}.
     *
     * @param path the full dotted path
     * @param value the current value
     * @param defaultValue the default value
     * @param comment the file comment, or {@code null} for none
     * @param validator the validator, or {@code null} for {@link Validator#none()}
     */
    public DoubleList {
      validator = validator != null ? validator : Validator.none();
    }

    /**
     * Creates a decimal list value with no comment and no validator.
     *
     * @param path the full dotted path, such as {@code "rewards.multipliers"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code List.of(...)}.
     */
    public DoubleList(java.lang.String path, List<java.lang.Double> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    /**
     * Creates a decimal list value with a comment and no validator.
     *
     * @param path the full dotted path, such as {@code "rewards.multipliers"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code List.of(...)}.
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
    public DoubleList(
        java.lang.String path, List<java.lang.Double> defaultValue, java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    /**
     * Creates a decimal list value with a validator and a comment.
     *
     * @param path the full dotted path, such as {@code "rewards.multipliers"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code List.of(...)}. It must pass {@code validator}, or loading fails.
     * @param validator checks the value, for example
     *     {@code Validator.allDoublesInRange(0.0, 10.0)}
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
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
   * A list of objects with named fields, read with {@link ConfigView#getMapList(java.lang.String)}.
   *
   * <pre>
   * points:
   *   - name: "point1"
   *     radius: 3.0
   *     duration: 100
   *   - name: "point2"
   *     radius: 5.0
   *     duration: 200
   * </pre>
   *
   * <p>Reads a YAML list whose elements are all maps. Keys become text; values stay as the YAML
   * parser gave them (String, Integer, Double, Boolean, List or Map), so check or convert each
   * field yourself, for example {@code ((Number) point.get("radius")).doubleValue()}. Nested
   * lists and maps are copied and unmodifiable, and keep the order they have in the file.
   *
   * @param path the full dotted path, such as {@code "capture.points"}
   * @param value the current value
   * @param defaultValue the value used when the file has no usable value at {@code path}
   * @param comment the comment written after the value in the file, or {@code null} for none
   * @param validator checks the value; {@code null} means {@link Validator#none()}
   */
  record MapList(
      java.lang.String path,
      List<Map<java.lang.String, Object>> value,
      List<Map<java.lang.String, Object>> defaultValue,
      java.lang.String comment,
      Validator<List<Map<java.lang.String, Object>>> validator)
      implements ConfigValue<List<Map<java.lang.String, Object>>> {

    /**
     * Creates a map list value with every field given. Most code uses the shorter
     * constructors, which set {@code value} to {@code defaultValue}.
     *
     * @param path the full dotted path
     * @param value the current value
     * @param defaultValue the default value
     * @param comment the file comment, or {@code null} for none
     * @param validator the validator, or {@code null} for {@link Validator#none()}
     */
    public MapList {
      validator = validator != null ? validator : Validator.none();
    }

    /**
     * Creates a map list value with no comment and no validator.
     *
     * @param path the full dotted path, such as {@code "capture.points"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code List.of(Map.of(...))}. Use only String, Number, Boolean, List and Map inside,
     *     or writing the file fails.
     */
    public MapList(java.lang.String path, List<Map<java.lang.String, Object>> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    /**
     * Creates a map list value with a comment and no validator.
     *
     * @param path the full dotted path, such as {@code "capture.points"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code List.of(Map.of(...))}. Use only String, Number, Boolean, List and Map inside,
     *     or writing the file fails.
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
    public MapList(
        java.lang.String path,
        List<Map<java.lang.String, Object>> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    /**
     * Creates a map list value with a validator and a comment.
     *
     * @param path the full dotted path, such as {@code "capture.points"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code List.of(Map.of(...))}. Use only String, Number, Boolean, List and Map inside,
     *     or writing the file fails. It must pass {@code validator}, or loading fails.
     * @param validator checks the value, for example {@code Validator.minSize(1)}
     * @param comment the comment written after the value in the file, or {@code null} for none
     */
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
   * One object with named fields of any YAML type, read with
   * {@link ConfigView#getMap(java.lang.String)}.
   *
   * <pre>
   * player-stats:
   *   health: 20
   *   mana: 100
   *   title: "Novice"
   * </pre>
   *
   * <p>Reads a YAML map. Keys become text; values stay as the YAML parser gave them (String,
   * Integer, Double, Boolean, List or Map). When every value has the same type, prefer
   * {@link StringMap} or {@link IntMap}, which convert and check each value for you. The map read
   * from the file is unmodifiable and keeps the file's key order.
   *
   * @param path the full dotted path, such as {@code "player-stats"}
   * @param value the current value
   * @param defaultValue the value used when the file has no usable value at {@code path}
   * @param comment the comment written after the value in the file, or {@code null} for none
   * @param validator checks the value; {@code null} means {@link Validator#none()}
   */
  record MapValue(
      java.lang.String path,
      Map<java.lang.String, Object> value,
      Map<java.lang.String, Object> defaultValue,
      java.lang.String comment,
      Validator<Map<java.lang.String, Object>> validator)
      implements ConfigValue<Map<java.lang.String, Object>> {

    /**
     * Creates a map value with every field given. Most code uses the shorter constructors,
     * which set {@code value} to {@code defaultValue}.
     *
     * @param path the full dotted path
     * @param value the current value
     * @param defaultValue the default value
     * @param comment the file comment, or {@code null} for none
     * @param validator the validator, or {@code null} for {@link Validator#none()}
     */
    public MapValue {
      validator = validator != null ? validator : Validator.none();
    }

    /**
     * Creates a map value with no comment and no validator.
     *
     * @param path the full dotted path, such as {@code "player-stats"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code Map.of(...)}, or a {@code LinkedHashMap} to control the order in the file. Use
     *     only String, Number, Boolean, List and Map values, or writing the file fails.
     */
    public MapValue(java.lang.String path, Map<java.lang.String, Object> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    /**
     * Creates a map value with a comment and no validator.
     *
     * @param path the full dotted path, such as {@code "player-stats"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code Map.of(...)}, or a {@code LinkedHashMap} to control the order in the file. Use
     *     only String, Number, Boolean, List and Map values, or writing the file fails.
     * @param comment the comment written after the key in the file, or {@code null} for none
     */
    public MapValue(
        java.lang.String path,
        Map<java.lang.String, Object> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    /**
     * Creates a map value with a validator and a comment.
     *
     * @param path the full dotted path, such as {@code "player-stats"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code Map.of(...)}, or a {@code LinkedHashMap} to control the order in the file. Use
     *     only String, Number, Boolean, List and Map values, or writing the file fails. It must
     *     pass {@code validator}, or loading fails.
     * @param validator checks the value, for example
     *     {@code Validator.of(m -> m.containsKey("health"), "health is required")}
     * @param comment the comment written after the key in the file, or {@code null} for none
     */
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
   * A map from text keys to text values, read with
   * {@link ConfigView#getStringMap(java.lang.String)}.
   *
   * <pre>
   * team-colors:
   *   red: "#FF0000"
   *   blue: "#0000FF"
   * </pre>
   *
   * <p>Reads a YAML map; each value follows the {@link String} rules, so numbers and booleans
   * become text. A list or map as a value rejects the map, and the error names the key.
   * The map read from the file is unmodifiable and keeps the file's key order.
   *
   * @param path the full dotted path, such as {@code "team-colors"}
   * @param value the current value
   * @param defaultValue the value used when the file has no usable value at {@code path}
   * @param comment the comment written after the value in the file, or {@code null} for none
   * @param validator checks the value; {@code null} means {@link Validator#none()}
   */
  record StringMap(
      java.lang.String path,
      Map<java.lang.String, java.lang.String> value,
      Map<java.lang.String, java.lang.String> defaultValue,
      java.lang.String comment,
      Validator<Map<java.lang.String, java.lang.String>> validator)
      implements ConfigValue<Map<java.lang.String, java.lang.String>> {

    /**
     * Creates a text map value with every field given. Most code uses the shorter
     * constructors, which set {@code value} to {@code defaultValue}.
     *
     * @param path the full dotted path
     * @param value the current value
     * @param defaultValue the default value
     * @param comment the file comment, or {@code null} for none
     * @param validator the validator, or {@code null} for {@link Validator#none()}
     */
    public StringMap {
      validator = validator != null ? validator : Validator.none();
    }

    /**
     * Creates a text map value with no comment and no validator.
     *
     * @param path the full dotted path, such as {@code "team-colors"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code Map.of(...)}, or a {@code LinkedHashMap} to control the order in the file.
     */
    public StringMap(java.lang.String path, Map<java.lang.String, java.lang.String> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    /**
     * Creates a text map value with a comment and no validator.
     *
     * @param path the full dotted path, such as {@code "team-colors"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code Map.of(...)}, or a {@code LinkedHashMap} to control the order in the file.
     * @param comment the comment written after the key in the file, or {@code null} for none
     */
    public StringMap(
        java.lang.String path,
        Map<java.lang.String, java.lang.String> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    /**
     * Creates a text map value with a validator and a comment.
     *
     * @param path the full dotted path, such as {@code "team-colors"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code Map.of(...)}, or a {@code LinkedHashMap} to control the order in the file. It
     *     must pass {@code validator}, or loading fails.
     * @param validator checks the value
     * @param comment the comment written after the key in the file, or {@code null} for none
     */
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
   * A map from text keys to whole numbers, read with
   * {@link ConfigView#getIntMap(java.lang.String)}.
   *
   * <pre>
   * class-levels:
   *   warrior: 10
   *   mage: 8
   * </pre>
   *
   * <p>Reads a YAML map; each value follows the {@link Int} rules. One bad value rejects the
   * map, and the error names the key. The map read from the file is unmodifiable and keeps
   * the file's key order.
   *
   * @param path the full dotted path, such as {@code "class-levels"}
   * @param value the current value
   * @param defaultValue the value used when the file has no usable value at {@code path}
   * @param comment the comment written after the value in the file, or {@code null} for none
   * @param validator checks the value; {@code null} means {@link Validator#none()}
   */
  record IntMap(
      java.lang.String path,
      Map<java.lang.String, Integer> value,
      Map<java.lang.String, Integer> defaultValue,
      java.lang.String comment,
      Validator<Map<java.lang.String, Integer>> validator)
      implements ConfigValue<Map<java.lang.String, Integer>> {

    /**
     * Creates an integer map value with every field given. Most code uses the shorter
     * constructors, which set {@code value} to {@code defaultValue}.
     *
     * @param path the full dotted path
     * @param value the current value
     * @param defaultValue the default value
     * @param comment the file comment, or {@code null} for none
     * @param validator the validator, or {@code null} for {@link Validator#none()}
     */
    public IntMap {
      validator = validator != null ? validator : Validator.none();
    }

    /**
     * Creates an integer map value with no comment and no validator.
     *
     * @param path the full dotted path, such as {@code "class-levels"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code Map.of(...)}, or a {@code LinkedHashMap} to control the order in the file.
     */
    public IntMap(java.lang.String path, Map<java.lang.String, Integer> defaultValue) {
      this(path, defaultValue, defaultValue, null, null);
    }

    /**
     * Creates an integer map value with a comment and no validator.
     *
     * @param path the full dotted path, such as {@code "class-levels"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code Map.of(...)}, or a {@code LinkedHashMap} to control the order in the file.
     * @param comment the comment written after the key in the file, or {@code null} for none
     */
    public IntMap(
        java.lang.String path,
        Map<java.lang.String, Integer> defaultValue,
        java.lang.String comment) {
      this(path, defaultValue, defaultValue, comment, null);
    }

    /**
     * Creates an integer map value with a validator and a comment.
     *
     * @param path the full dotted path, such as {@code "class-levels"}
     * @param defaultValue the default, also used as the starting value. Stored as given; pass
     *     {@code Map.of(...)}, or a {@code LinkedHashMap} to control the order in the file. It
     *     must pass {@code validator}, or loading fails.
     * @param validator checks the value
     * @param comment the comment written after the key in the file, or {@code null} for none
     */
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
