package me.mortaldev.abstractyaml;

/**
 * One value of a legacy config file: its path, Java type, current value, default, and optional
 * validator and comment. Read it into an {@link AbstractConfig} with
 * {@link AbstractConfig#getConfigValue(ConfigValue)}:
 *
 * <pre>{@code
 * cooldown = getConfigValue(new ConfigValue<>("cooldown", Integer.class, 10)
 *     .setValidator(ConfigValidator.range(1, 300))
 *     .setComment("Seconds between uses"));
 * int seconds = cooldown.getValue();
 * }</pre>
 *
 * <p>The type is passed as a class ({@code Integer.class}, {@code String.class},
 * {@code List.class}, {@code Map.class}) because generic types are erased at runtime. Use
 * wrapper classes, not primitives: {@code Integer.class}, not {@code int.class}.
 *
 * <p>Unlike config2 values, this object is mutable and is changed by loading. It is not
 * thread-safe.
 *
 * @param <T> the type of the configuration value
 * @deprecated Part of the legacy config system. Use {@code me.mortaldev.abstractyaml.config2.ConfigValue}.
 */
@Deprecated
public class ConfigValue<T> {
  private final String id;
  private T value;
  private T defaultValue;
  private final Class<T> valueType;
  private ConfigValidator<T> validator;
  private String comment;

  /**
   * Creates a new ConfigValue with explicit type information.
   *
   * <p>This constructor should be preferred as it avoids type erasure issues.
   *
   * @param id the configuration path/key
   * @param valueType the class type of the value (e.g., String.class, Integer.class)
   * @param value the initial value
   * @param defaultValue the default value to use if the config value is invalid
   */
  public ConfigValue(String id, Class<T> valueType, T value, T defaultValue) {
    this.id = id;
    this.valueType = valueType;
    this.value = value;
    this.defaultValue = defaultValue;
  }

  /**
   * Creates a new ConfigValue with explicit type information and default initial value.
   *
   * @param id the configuration path/key
   * @param valueType the class type of the value
   * @param defaultValue the default value (also used as initial value)
   */
  public ConfigValue(String id, Class<T> valueType, T defaultValue) {
    this(id, valueType, defaultValue, defaultValue);
  }

  /**
   * Returns the path of this value in the file, such as {@code "game.max-players"}.
   *
   * @return the path given to the constructor
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the type the file's value must have.
   *
   * @return the class given to the constructor
   */
  public Class<T> getValueType() {
    return valueType;
  }

  /**
   * Returns the current value: the one read by
   * {@link AbstractConfig#getConfigValue(ConfigValue)}, or the last one passed to
   * {@link #setValue(Object)}.
   *
   * @return the current value
   */
  public T getValue() {
    return value;
  }

  /**
   * Returns the value used when the file's value is missing, of the wrong type or invalid.
   *
   * @return the default value
   */
  public T getDefaultValue() {
    return defaultValue;
  }

  /**
   * Changes the current value in memory only, without validation. Call
   * {@link AbstractConfig#saveValue(ConfigValue)} to validate it and write it to the file.
   *
   * @param value the new value
   */
  public void setValue(T value) {
    this.value = value;
  }

  /**
   * Changes the default value. The current value is not changed.
   *
   * @param defaultValue the new default
   */
  public void setDefaultValue(T defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * Sets the validator that loading and {@link AbstractConfig#saveValue(ConfigValue)} check the
   * value with. Replaces any earlier validator.
   *
   * @param validator the validator to use, or {@code null} for none
   * @return this ConfigValue for method chaining
   */
  public ConfigValue<T> setValidator(ConfigValidator<T> validator) {
    this.validator = validator;
    return this;
  }

  /**
   * Gets the validator for this config value, if set.
   *
   * @return the validator, or null if none is set
   */
  public ConfigValidator<T> getValidator() {
    return validator;
  }

  /**
   * Validates the current value against the validator if one is set.
   *
   * @return the validation result, or a successful result if no validator is set
   */
  public ConfigValidator.ValidationResult validate() {
    if (validator == null) {
      return new ConfigValidator.ValidationResult(true, java.util.Collections.emptyList());
    }
    return validator.validate(value);
  }

  /**
   * Checks if the current value is valid according to the validator.
   *
   * @return true if valid or no validator is set, false otherwise
   */
  public boolean isValid() {
    return validate().isValid();
  }

  /**
   * Sets the comment written after this value when the file is generated.
   *
   * @param comment the comment to display inline with the value, without {@code #}
   * @return this ConfigValue for method chaining
   */
  public ConfigValue<T> setComment(String comment) {
    this.comment = comment;
    return this;
  }

  /**
   * Gets the inline comment for this config value.
   *
   * @return the comment, or null if none is set
   */
  public String getComment() {
    return comment;
  }

  @Override
  public String toString() {
    return "ConfigValue{id='"
        + id
        + "', value="
        + value
        + ", type="
        + valueType.getSimpleName()
        + "}";
  }
}
