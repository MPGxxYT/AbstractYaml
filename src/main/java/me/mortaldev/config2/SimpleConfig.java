package me.mortaldev.config2;

import java.io.File;
import java.util.*;

/**
 * Simple immutable implementation of Config.
 */
class SimpleConfig implements Config {
  private final ConfigSchema schema;
  private final Map<String, ConfigValue<?>> values;

  SimpleConfig(ConfigSchema schema, Map<String, ConfigValue<?>> values) {
    this.schema = schema;
    this.values = Map.copyOf(values); // Immutable copy
  }

  @Override
  public ConfigSchema schema() {
    return schema;
  }

  @Override
  public String name() {
    return schema.name();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<ConfigValue<T>> get(String path) {
    return Optional.ofNullable((ConfigValue<T>) values.get(path));
  }

  @Override
  public <T> ConfigValue<T> getOrThrow(String path) {
    return this.<T>get(path)
        .orElseThrow(() -> new IllegalArgumentException("No config value at path: " + path));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getValue(String path) {
    return get(path).map(ConfigValue::value);
  }

  @Override
  public int getInt(String path) {
    ConfigValue<?> value = getOrThrow(path);
    if (!(value instanceof ConfigValue.Int)) {
      throw new IllegalArgumentException("Value at " + path + " is not an integer");
    }
    return ((ConfigValue.Int) value).value();
  }

  @Override
  public double getDouble(String path) {
    ConfigValue<?> value = getOrThrow(path);
    if (!(value instanceof ConfigValue.Double)) {
      throw new IllegalArgumentException("Value at " + path + " is not a double");
    }
    return ((ConfigValue.Double) value).value();
  }

  @Override
  public String getString(String path) {
    ConfigValue<?> value = getOrThrow(path);
    if (!(value instanceof ConfigValue.String)) {
      throw new IllegalArgumentException("Value at " + path + " is not a string");
    }
    return ((ConfigValue.String) value).value();
  }

  @Override
  public boolean getBoolean(String path) {
    ConfigValue<?> value = getOrThrow(path);
    if (!(value instanceof ConfigValue.Boolean)) {
      throw new IllegalArgumentException("Value at " + path + " is not a boolean");
    }
    return ((ConfigValue.Boolean) value).value();
  }

  @Override
  public List<String> getStringList(String path) {
    ConfigValue<?> value = getOrThrow(path);
    if (!(value instanceof ConfigValue.StringList)) {
      throw new IllegalArgumentException("Value at " + path + " is not a string list");
    }
    return ((ConfigValue.StringList) value).value();
  }

  @Override
  public List<Integer> getIntList(String path) {
    ConfigValue<?> value = getOrThrow(path);
    if (!(value instanceof ConfigValue.IntList)) {
      throw new IllegalArgumentException("Value at " + path + " is not an integer list");
    }
    return ((ConfigValue.IntList) value).value();
  }

  @Override
  public List<Double> getDoubleList(String path) {
    ConfigValue<?> value = getOrThrow(path);
    if (!(value instanceof ConfigValue.DoubleList)) {
      throw new IllegalArgumentException("Value at " + path + " is not a double list");
    }
    return ((ConfigValue.DoubleList) value).value();
  }

  @Override
  public Map<String, ConfigValue<?>> allValues() {
    return values;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Config with(String path, T newValue) {
    ConfigValue<?> existing = getOrThrow(path);
    ConfigValue<T> typedExisting = (ConfigValue<T>) existing;
    ConfigValue<T> updated = typedExisting.withValue(newValue);

    // Validate the new value
    ValidationResult validation = updated.validate();
    if (!validation.isValid()) {
      throw new IllegalArgumentException(
          "Invalid value for " + path + ": " + validation.errorMessage());
    }

    Map<String, ConfigValue<?>> newValues = new HashMap<>(values);
    newValues.put(path, updated);
    return new SimpleConfig(schema, newValues);
  }

  @Override
  public Config withAll(Map<String, Object> updates) {
    Config result = this;
    for (Map.Entry<String, Object> entry : updates.entrySet()) {
      result = result.with(entry.getKey(), entry.getValue());
    }
    return result;
  }

  @Override
  public ValidationResult validate() {
    List<String> errors = new ArrayList<>();
    for (ConfigValue<?> value : values.values()) {
      ValidationResult result = value.validate();
      if (!result.isValid()) {
        errors.add(value.path() + ": " + result.errorMessage());
      }
    }
    return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
  }

  @Override
  public void save(File file) {
    YamlConfigWriter.write(this, file);
  }

  @Override
  public ConfigSection section(String prefix) {
    return new SimpleConfigSection(this, prefix);
  }

  /**
   * Simple implementation of ConfigSection.
   */
  private static class SimpleConfigSection implements ConfigSection {
    private final Config config;
    private final String prefix;

    SimpleConfigSection(Config config, String prefix) {
      this.config = config;
      this.prefix = prefix;
    }

    @Override
    public String prefix() {
      return prefix;
    }

    @Override
    public Config config() {
      return config;
    }

    private String fullPath(String relativePath) {
      return prefix + "." + relativePath;
    }

    @Override
    public <T> ConfigValue<T> get(String relativePath) {
      return config.getOrThrow(fullPath(relativePath));
    }

    @Override
    public int getInt(String relativePath) {
      return config.getInt(fullPath(relativePath));
    }

    @Override
    public double getDouble(String relativePath) {
      return config.getDouble(fullPath(relativePath));
    }

    @Override
    public String getString(String relativePath) {
      return config.getString(fullPath(relativePath));
    }

    @Override
    public boolean getBoolean(String relativePath) {
      return config.getBoolean(fullPath(relativePath));
    }

    @Override
    public List<String> getStringList(String relativePath) {
      return config.getStringList(fullPath(relativePath));
    }

    @Override
    public List<Integer> getIntList(String relativePath) {
      return config.getIntList(fullPath(relativePath));
    }

    @Override
    public List<Double> getDoubleList(String relativePath) {
      return config.getDoubleList(fullPath(relativePath));
    }

    @Override
    public <T> Config set(String relativePath, T newValue) {
      return config.with(fullPath(relativePath), newValue);
    }
  }
}
