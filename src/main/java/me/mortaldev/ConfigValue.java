package me.mortaldev;

/**
 * Represents a configuration value with type-safe operations.
 *
 * <p>This class provides a wrapper around configuration values that maintains type information
 * at runtime, allowing for safe retrieval and storage of configuration data.
 *
 * @param <T> the type of the configuration value
 */
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
   * Creates a new ConfigValue without explicit type information.
   *
   * @deprecated Use {@link #ConfigValue(String, Class, Object, Object)} instead to avoid type erasure issues.
   * This constructor attempts to infer the type from the values, which may not work correctly
   * for generic types like List&lt;String&gt;.
   */
  @Deprecated
  public ConfigValue(String id, T value, T defaultValue) {
    this.id = id;
    this.value = value;
    this.defaultValue = defaultValue;

    // IMPROVEMENT: Determine the type from the value, falling back to the default value.
    if (value != null) {
      this.valueType = (Class<T>) value.getClass();
    } else if (defaultValue != null) {
      this.valueType = (Class<T>) defaultValue.getClass();
    } else {
      // This is an unrecoverable state for the generic helper.
      throw new IllegalArgumentException(
          "Cannot determine type for ConfigValue '"
              + id
              + "' because both the initial and default values are null.");
    }
  }

  /**
   * Creates a new ConfigValue without explicit type information.
   *
   * @deprecated Use {@link #ConfigValue(String, Class, Object)} instead.
   */
  @Deprecated
  public ConfigValue(String id, T defaultValue) {
    this(id, defaultValue, defaultValue);
  }

  public String getId() {
    return id;
  }

  public Class<T> getValueType() {
    return valueType;
  }

  public T getValue() {
    return value;
  }

  public T getDefaultValue() {
    return defaultValue;
  }

  public void setValue(T value) {
    this.value = value;
  }

  public void setDefaultValue(T defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * Sets a validator for this config value.
   *
   * @param validator the validator to use
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
   * Sets an inline comment for this config value.
   *
   * @param comment the comment to display inline with the value
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
    return "ConfigValue{id='" + id + "', value=" + value + ", type=" + valueType.getSimpleName() + "}";
  }
}
