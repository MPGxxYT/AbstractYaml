package me.mortaldev.config2;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads configurations from a {@link ConfigSource}.
 * Supports auto-generation of missing files and validation.
 */
public class ConfigLoader {
  private final ConfigSchema schema;
  private final ConfigSource source;
  private final boolean autoGenerate;
  private final boolean validateOnLoad;
  private final Logger logger;

  private ConfigLoader(Builder builder) {
    this.schema = builder.schema;
    this.source = builder.source;
    this.autoGenerate = builder.autoGenerate;
    this.validateOnLoad = builder.validateOnLoad;
    this.logger = builder.logger;
  }

  /**
   * Loads the configuration.
   * If nothing is stored yet and autoGenerate is true, the defaults are written.
   *
   * @return the loaded configuration
   * @throws ConfigLoadException if validation fails
   */
  public Config load() {
    String text = source.read();

    if (text == null) {
      Config defaults = createDefaultConfig();
      checkValid(defaults, "Default values of " + schema.name() + " fail validation");
      if (autoGenerate) {
        logger.info("Config file " + source.name() + " not found, generating with defaults...");
        source.write(defaults.toYaml());
      }
      return defaults;
    }

    YamlConfiguration yaml = new YamlConfiguration();
    try {
      yaml.loadFromString(text);
    } catch (InvalidConfigurationException e) {
      logger.severe(
          "Could not parse " + source.name() + ", using defaults: " + e.getMessage());
      return createDefaultConfig();
    }

    Map<String, ConfigValue<?>> loadedValues = new LinkedHashMap<>();
    for (ConfigValue<?> schemaValue : schema.values()) {
      String path = schemaValue.path();

      if (!yaml.contains(path)) {
        logger.warning(
            "Config value '" + path + "' missing from " + source.name() + ", using default");
        loadedValues.put(path, schemaValue);
        continue;
      }

      loadedValues.put(path, loadValue(yaml, schemaValue));
    }

    Config config = new SimpleConfig(schema, loadedValues);
    checkValid(config, "Configuration validation failed for " + source.name());
    return config;
  }

  private void checkValid(Config config, String message) {
    if (!validateOnLoad) {
      return;
    }
    ValidationResult validation = config.validate();
    if (!validation.isValid()) {
      throw new ConfigLoadException(message + ":\n" + String.join("\n", validation.errors()));
    }
  }

  /**
   * Loads a specific value from the YAML configuration.
   */
  private ConfigValue<?> loadValue(YamlConfiguration yaml, ConfigValue<?> schemaValue) {
    String path = schemaValue.path();
    try {
      return schemaValue.read(toPlain(yaml.get(path)));
    } catch (IllegalArgumentException e) {
      logger.warning(
          "Invalid value '" + path + "' in " + source.name() +
          ": " + e.getMessage() + ". Using default.");
      return schemaValue;
    }
  }

  /** Converts Bukkit's ConfigurationSection nodes into plain maps. */
  private static Object toPlain(Object raw) {
    if (raw instanceof ConfigurationSection section) {
      Map<String, Object> map = new LinkedHashMap<>();
      for (String key : section.getKeys(false)) {
        map.put(key, toPlain(section.get(key)));
      }
      return map;
    }
    if (raw instanceof List<?> list) {
      List<Object> copy = new ArrayList<>(list.size());
      for (Object item : list) {
        copy.add(toPlain(item));
      }
      return copy;
    }
    return raw;
  }

  /**
   * Creates a config with all default values.
   */
  private Config createDefaultConfig() {
    Map<String, ConfigValue<?>> defaultValues = new LinkedHashMap<>();
    for (ConfigValue<?> value : schema.values()) {
      defaultValues.put(value.path(), value);
    }
    return new SimpleConfig(schema, defaultValues);
  }

  /**
   * Reloads the configuration.
   * Returns a fresh config instance.
   */
  public Config reload() {
    return load();
  }

  /**
   * The source this loader reads from.
   */
  public ConfigSource source() {
    return source;
  }

  // ===== Builder =====

  /**
   * Creates a builder that resolves files and logging from a plugin.
   */
  public static Builder builder(JavaPlugin plugin) {
    return new Builder(plugin);
  }

  /**
   * Creates a builder without a plugin. Set a {@link Builder#source(ConfigSource) source} or an
   * absolute {@link Builder#file(File) file}.
   */
  public static Builder builder() {
    return new Builder(null);
  }

  public static class Builder {
    private final JavaPlugin plugin;
    private ConfigSchema schema;
    private ConfigSource source;
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
     * Sets where the YAML text is read from and written to.
     */
    public Builder source(ConfigSource source) {
      this.source = source;
      return this;
    }

    /**
     * Sets the file to load from.
     * If not specified, uses plugin.getDataFolder() / schema.name() + ".yml"
     */
    public Builder file(File file) {
      return source(ConfigSource.file(file));
    }

    /**
     * Sets the file by name (relative to plugin data folder).
     */
    public Builder file(String name) {
      if (!name.endsWith(".yml")) {
        name = name + ".yml";
      }
      return file(new File(requirePlugin("file(String)").getDataFolder(), name));
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
      if (source == null) {
        file(new File(requirePlugin("a default file").getDataFolder(), schema.name() + ".yml"));
      }
      if (logger == null) {
        logger = plugin != null ? plugin.getLogger() : Logger.getLogger(schema.name());
      }
      return new ConfigLoader(this);
    }

    /**
     * Builds the loader and immediately loads the config.
     */
    public Config load() {
      return build().load();
    }

    private JavaPlugin requirePlugin(String feature) {
      if (plugin == null) {
        throw new IllegalStateException(
            feature + " needs a plugin; use ConfigLoader.builder(plugin) or set a source");
      }
      return plugin;
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
