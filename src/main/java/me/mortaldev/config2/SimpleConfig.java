package me.mortaldev.config2;

import java.io.File;
import java.util.*;

/** Simple immutable implementation of Config. */
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
    return this.<T>get(path).map(ConfigValue::value);
  }

  @Override
  public int getInt(String path) {
    return typed(path, ConfigValue.Int.class, "an integer");
  }

  @Override
  public double getDouble(String path) {
    return typed(path, ConfigValue.Double.class, "a double");
  }

  @Override
  public String getString(String path) {
    return typed(path, ConfigValue.String.class, "a string");
  }

  @Override
  public boolean getBoolean(String path) {
    return typed(path, ConfigValue.Boolean.class, "a boolean");
  }

  @Override
  public List<String> getStringList(String path) {
    return typed(path, ConfigValue.StringList.class, "a string list");
  }

  @Override
  public List<Integer> getIntList(String path) {
    return typed(path, ConfigValue.IntList.class, "an integer list");
  }

  @Override
  public List<Double> getDoubleList(String path) {
    return typed(path, ConfigValue.DoubleList.class, "a double list");
  }

  @Override
  public List<Map<String, Object>> getMapList(String path) {
    return typed(path, ConfigValue.MapList.class, "a map list");
  }

  @Override
  public Map<String, Object> getMap(String path) {
    return typed(path, ConfigValue.MapValue.class, "a map");
  }

  @Override
  public Map<String, String> getStringMap(String path) {
    return typed(path, ConfigValue.StringMap.class, "a string map");
  }

  @Override
  public Map<String, Integer> getIntMap(String path) {
    return typed(path, ConfigValue.IntMap.class, "an integer map");
  }

  private <R, V extends ConfigValue<R>> R typed(String path, Class<V> kind, String description) {
    ConfigValue<?> value = getOrThrow(path);
    if (!kind.isInstance(value)) {
      throw new IllegalArgumentException("Value at " + path + " is not " + description);
    }
    return kind.cast(value).value();
  }

  @Override
  public Map<String, ConfigValue<?>> allValues() {
    return values;
  }

  @Override
  public <T> Config with(String path, T newValue) {
    ConfigValue<?> existing = getOrThrow(path);
    ConfigValue<?> updated;
    try {
      updated = existing.read(newValue);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid value for " + path + ": " + e.getMessage(), e);
    }

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

  /** Simple implementation of ConfigSection. */
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
    public List<Map<String, Object>> getMapList(String relativePath) {
      return config.getMapList(fullPath(relativePath));
    }

    @Override
    public Map<String, Object> getMap(String relativePath) {
      return config.getMap(fullPath(relativePath));
    }

    @Override
    public Map<String, String> getStringMap(String relativePath) {
      return config.getStringMap(fullPath(relativePath));
    }

    @Override
    public Map<String, Integer> getIntMap(String relativePath) {
      return config.getIntMap(fullPath(relativePath));
    }

    @Override
    public <T> Config set(String relativePath, T newValue) {
      return config.with(fullPath(relativePath), newValue);
    }
  }
}
