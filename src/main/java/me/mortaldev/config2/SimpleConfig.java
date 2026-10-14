package me.mortaldev.config2;

import java.util.*;

/** Simple immutable implementation of Config. */
class SimpleConfig implements Config {
  private final ConfigSchema schema;
  private final Map<String, ConfigValue<?>> values;

  SimpleConfig(ConfigSchema schema, Map<String, ConfigValue<?>> values) {
    this.schema = schema;
    // Immutable copy that keeps declaration order
    this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
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

    Map<String, ConfigValue<?>> newValues = new LinkedHashMap<>(values);
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
  public ConfigSection section(String prefix) {
    return new SimpleConfigSection(this, prefix);
  }

  /** A prefix-scoped view of a SimpleConfig. */
  private record SimpleConfigSection(Config config, String prefix) implements ConfigSection {

    private String fullPath(String relativePath) {
      return prefix + "." + relativePath;
    }

    @Override
    public <T> ConfigValue<T> get(String relativePath) {
      return config.getOrThrow(fullPath(relativePath));
    }

    @Override
    public <T> ConfigValue<T> getOrThrow(String relativePath) {
      return config.getOrThrow(fullPath(relativePath));
    }

    @Override
    public <T> Optional<T> getValue(String relativePath) {
      return config.getValue(fullPath(relativePath));
    }

    @Override
    public ConfigSection section(String relativePrefix) {
      return new SimpleConfigSection(config, fullPath(relativePrefix));
    }

    @Override
    public <T> Config set(String relativePath, T newValue) {
      return config.with(fullPath(relativePath), newValue);
    }
  }
}
