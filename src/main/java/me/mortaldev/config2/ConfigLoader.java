package me.mortaldev.config2;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads configuration files from disk.
 * Supports auto-generation of missing files and validation.
 */
public class ConfigLoader {
  private final JavaPlugin plugin;
  private final ConfigSchema schema;
  private final File file;
  private final boolean autoGenerate;
  private final boolean validateOnLoad;
  private final Logger logger;

  private ConfigLoader(Builder builder) {
    this.plugin = builder.plugin;
    this.schema = builder.schema;
    this.file = builder.file;
    this.autoGenerate = builder.autoGenerate;
    this.validateOnLoad = builder.validateOnLoad;
    this.logger = builder.logger != null ? builder.logger : plugin.getLogger();
  }

  /**
   * Loads the configuration from disk.
   * If the file doesn't exist and autoGenerate is true, it will be created with defaults.
   *
   * @return the loaded configuration
   * @throws ConfigLoadException if loading fails
   */
  public Config load() {
    // Create parent directory if needed
    if (!file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }

    // Auto-generate if file doesn't exist
    if (!file.exists() && autoGenerate) {
      logger.info("Config file " + file.getName() + " not found, generating with defaults...");
      Config defaultConfig = createDefaultConfig();
      defaultConfig.save(file);
      return defaultConfig;
    }

    // Load from file
    FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
    Map<String, ConfigValue<?>> loadedValues = new HashMap<>();

    // Load each value from the YAML file
    for (ConfigValue<?> schemaValue : schema.values()) {
      String path = schemaValue.path();

      if (!yaml.contains(path)) {
        // Value missing from file, use default
        logger.warning(
            "Config value '" + path + "' missing from " + file.getName() + ", using default");
        loadedValues.put(path, schemaValue);
        continue;
      }

      // Load value based on type
      ConfigValue<?> loadedValue = loadValue(yaml, schemaValue);
      loadedValues.put(path, loadedValue);
    }

    Config config = new SimpleConfig(schema, loadedValues);

    // Validate if requested
    if (validateOnLoad) {
      ValidationResult validation = config.validate();
      if (!validation.isValid()) {
        throw new ConfigLoadException(
            "Configuration validation failed for " + file.getName() + ":\n" +
            String.join("\n", validation.errors()));
      }
    }

    return config;
  }

  /**
   * Loads a specific value from the YAML configuration.
   */
  @SuppressWarnings("unchecked")
  private ConfigValue<?> loadValue(FileConfiguration yaml, ConfigValue<?> schemaValue) {
    String path = schemaValue.path();

    try {
      if (schemaValue instanceof ConfigValue.Int intValue) {
        int value = yaml.getInt(path, intValue.defaultValue());
        return intValue.withValue(value);
      } else if (schemaValue instanceof ConfigValue.Double doubleValue) {
        double value = yaml.getDouble(path, doubleValue.defaultValue());
        return doubleValue.withValue(value);
      } else if (schemaValue instanceof ConfigValue.String stringValue) {
        String value = yaml.getString(path, stringValue.defaultValue());
        return stringValue.withValue(value);
      } else if (schemaValue instanceof ConfigValue.Boolean boolValue) {
        boolean value = yaml.getBoolean(path, boolValue.defaultValue());
        return boolValue.withValue(value);
      } else if (schemaValue instanceof ConfigValue.StringList listValue) {
        List<String> value = yaml.getStringList(path);
        if (value.isEmpty() && !listValue.defaultValue().isEmpty()) {
          value = listValue.defaultValue();
        }
        return listValue.withValue(value);
      } else if (schemaValue instanceof ConfigValue.IntList listValue) {
        List<Integer> value = yaml.getIntegerList(path);
        if (value.isEmpty() && !listValue.defaultValue().isEmpty()) {
          value = listValue.defaultValue();
        }
        return listValue.withValue(value);
      } else if (schemaValue instanceof ConfigValue.DoubleList listValue) {
        List<Double> value = yaml.getDoubleList(path);
        if (value.isEmpty() && !listValue.defaultValue().isEmpty()) {
          value = listValue.defaultValue();
        }
        return listValue.withValue(value);
      }
    } catch (Exception e) {
      logger.warning(
          "Failed to load value '" + path + "' from " + file.getName() +
          ": " + e.getMessage() + ". Using default.");
      return schemaValue;
    }

    return schemaValue;
  }

  /**
   * Creates a config with all default values.
   */
  private Config createDefaultConfig() {
    Map<String, ConfigValue<?>> defaultValues = new HashMap<>();
    for (ConfigValue<?> value : schema.values()) {
      defaultValues.put(value.path(), value);
    }
    return new SimpleConfig(schema, defaultValues);
  }

  /**
   * Reloads the configuration from disk.
   * Returns a fresh config instance.
   */
  public Config reload() {
    return load();
  }

  // ===== Builder =====

  public static Builder builder(JavaPlugin plugin) {
    return new Builder(plugin);
  }

  public static class Builder {
    private final JavaPlugin plugin;
    private ConfigSchema schema;
    private File file;
    private boolean autoGenerate = true;
    private boolean validateOnLoad = true;
    private Logger logger;

    private Builder(JavaPlugin plugin) {
      this.plugin = plugin;
    }

    /**
     * Sets the schema for this configuration.
     */
    public Builder schema(ConfigSchema schema) {
      this.schema = schema;
      return this;
    }

    /**
     * Sets the file to load from.
     * If not specified, uses plugin.getDataFolder() / schema.name() + ".yml"
     */
    public Builder file(File file) {
      this.file = file;
      return this;
    }

    /**
     * Sets the file by name (relative to plugin data folder).
     */
    public Builder file(String name) {
      if (!name.endsWith(".yml")) {
        name = name + ".yml";
      }
      this.file = new File(plugin.getDataFolder(), name);
      return this;
    }

    /**
     * Whether to auto-generate the file if it doesn't exist.
     * Default: true
     */
    public Builder autoGenerate(boolean autoGenerate) {
      this.autoGenerate = autoGenerate;
      return this;
    }

    /**
     * Whether to validate the config after loading.
     * If validation fails, throws ConfigLoadException.
     * Default: true
     */
    public Builder validateOnLoad(boolean validateOnLoad) {
      this.validateOnLoad = validateOnLoad;
      return this;
    }

    /**
     * Sets a custom logger.
     * If not specified, uses the plugin's logger.
     */
    public Builder logger(Logger logger) {
      this.logger = logger;
      return this;
    }

    /**
     * Builds and returns the loader (does not load the config yet).
     */
    public ConfigLoader build() {
      if (schema == null) {
        throw new IllegalStateException("Schema must be set");
      }
      if (file == null) {
        file = new File(plugin.getDataFolder(), schema.name() + ".yml");
      }
      return new ConfigLoader(this);
    }

    /**
     * Builds the loader and immediately loads the config.
     */
    public Config load() {
      return build().load();
    }
  }

  /**
   * Exception thrown when config loading fails.
   */
  public static class ConfigLoadException extends RuntimeException {
    public ConfigLoadException(String message) {
      super(message);
    }

    public ConfigLoadException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
