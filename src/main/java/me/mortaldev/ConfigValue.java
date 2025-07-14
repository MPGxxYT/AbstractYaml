package me.mortaldev;

public class ConfigValue<T> {
  private final String id;
  private T value;
  private T defaultValue;
  private final Class<T> valueType;

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
}