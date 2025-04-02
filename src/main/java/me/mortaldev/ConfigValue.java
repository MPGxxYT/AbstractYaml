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
    if (value != null) {
      this.valueType = (Class<T>) value.getClass();
    } else {
      this.valueType = null;
      System.out.println("Error casting value to class.");
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
