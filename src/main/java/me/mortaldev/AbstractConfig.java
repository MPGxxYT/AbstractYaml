package me.mortaldev;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Abstract base class for configuration management with caching support.
 *
 * <p>This class provides a framework for loading, saving, and accessing configuration values
 * with built-in caching to reduce disk I/O operations.
 *
 * @deprecated Part of the legacy config system, kept for existing plugins. New code should use
 *     {@code me.mortaldev.config2}: a {@code ConfigSchema} subclass registered with
 *     {@code ConfigManager}. See {@code config2/MIGRATION_GUIDE.md}.
 */
@Deprecated
public abstract class AbstractConfig {
  private static final String EXTENSION = ".yml";
  private FileConfiguration config;

  // Cache for ConfigValue objects to avoid repeated disk reads
  private final Map<String, Object> valueCache = new ConcurrentHashMap<>();
  private boolean cacheEnabled = true;

  // Track all ConfigValues for auto-generation
  private final Map<String, ConfigValue<?>> trackedValues = new LinkedHashMap<>();
  private final Map<String, ConfigValueContainer> trackedContainers = new LinkedHashMap<>();
  private String fileHeader;

  /**
   * Logs a message to the console.
   *
   * @param message the message to log
   */
  public abstract void log(String message);

  /**
   * Returns the name of this configuration file, without the extension.
   *
   * @return the name of this configuration file
   */
  public abstract String getName();

  /**
   * Returns the main plugin for this configuration.
   *
   * @return the main plugin
   */
  public abstract JavaPlugin getMain();

  /**
   * Called after the configuration is loaded to initialize config values.
   * Implementations should load their ConfigValue fields here.
   */
  public abstract void loadData();

  public AbstractConfig() {}

  /**
   * Enables or disables caching for this configuration.
   *
   * @param enabled true to enable caching, false to disable
   */
  public void setCacheEnabled(boolean enabled) {
    this.cacheEnabled = enabled;
    if (!enabled) {
      clearCache();
    }
  }

  /**
   * Checks if caching is enabled for this configuration.
   *
   * @return true if caching is enabled
   */
  public boolean isCacheEnabled() {
    return cacheEnabled;
  }

  /**
   * Clears the value cache, forcing the next access to read from disk.
   */
  public void clearCache() {
    valueCache.clear();
  }

  /**
   * Clears a specific value from the cache.
   *
   * @param path the configuration path to clear from cache
   */
  public void clearCacheValue(String path) {
    valueCache.remove(path);
  }

  /**
   * Retrieves and populates a ConfigValue from the configuration.
   *
   * @param configValue the ConfigValue to populate
   * @return the populated ConfigValue
   * @param <T> the type of the value
   */
  public <T> ConfigValue<T> getConfigValue(ConfigValue<T> configValue) {
    Class<T> valueType = configValue.getValueType();
    String path = configValue.getId();
    T defaultValue = configValue.getDefaultValue();

    // Track this value for auto-generation
    trackedValues.put(path, configValue);

    // Check cache first if enabled; ignore entries of the wrong type
    if (cacheEnabled) {
      Object cached = valueCache.get(path);
      if (valueType.isInstance(cached)) {
        configValue.setValue(valueType.cast(cached));
        return configValue;
      }
    }

    Object rawValue = config.get(path, defaultValue);
    if (Map.class.isAssignableFrom(valueType) && rawValue instanceof ConfigurationSection section) {
      // The config has a section, and we expect a map. Convert it.
      rawValue = toMap(section);
    }

    T value = coerce(rawValue, valueType);
    if (value == null) {
      logger()
          .warning(
              "Config Value for '"
                  + path
                  + "' is invalid. Found type "
                  + (rawValue != null ? rawValue.getClass().getSimpleName() : "null")
                  + " but expected "
                  + valueType.getSimpleName()
                  + ". Setting to default: "
                  + defaultValue);
      configValue.setValue(defaultValue);
      saveValue(configValue);
    } else {
      configValue.setValue(value);
      ConfigValidator.ValidationResult result = configValue.validate();
      if (!result.isValid()) {
        logger()
            .warning(
                "Config Value for '"
                    + path
                    + "' failed validation: "
                    + result.getErrorMessage()
                    + ". Setting to default: "
                    + defaultValue);
        configValue.setValue(defaultValue);
        saveValue(configValue);
      }
    }

    if (cacheEnabled) {
      valueCache.put(path, configValue.getValue());
    }
    return configValue;
  }

  /**
   * Converts a raw YAML value to {@code type}, or returns {@code null} if it can't be done.
   * Numbers convert between Integer, Long, Float and Double when no information is lost, so a
   * YAML {@code 5} reads as a Double and {@code 5.0} reads as an Integer.
   */
  static <T> T coerce(Object raw, Class<T> type) {
    if (raw == null) {
      return null;
    }
    if (type.isInstance(raw)) {
      return type.cast(raw);
    }
    if (!(raw instanceof Number number)) {
      return null;
    }
    double d = number.doubleValue();
    boolean whole = d == Math.rint(d) && !Double.isInfinite(d);
    if (type == Double.class) {
      return type.cast(d);
    }
    if (type == Float.class) {
      return type.cast(number.floatValue());
    }
    if (type == Long.class && whole && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
      return type.cast(number.longValue());
    }
    if (type == Integer.class && whole && d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE) {
      return type.cast(number.intValue());
    }
    return null;
  }

  /** Converts a section to nested plain maps, keeping key order. */
  private static Map<String, Object> toMap(ConfigurationSection section) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (String key : section.getKeys(false)) {
      Object value = section.get(key);
      map.put(key, value instanceof ConfigurationSection child ? toMap(child) : value);
    }
    return map;
  }

  private Logger logger() {
    JavaPlugin main = getMain();
    return main != null ? main.getLogger() : Logger.getLogger(getName());
  }

  /** Uses an already loaded configuration, for tests that run without a plugin. */
  void useConfig(FileConfiguration config) {
    this.config = config;
    clearCache();
  }

  public String failedToLoad(String configName, String configValue) {
    return YAML.getInstance().failedToLoad(getMain(), configName, configValue);
  }

  public String failedToLoad(String configName, String configValue, String failReason) {
    return YAML.getInstance().failedToLoad(getMain(), configName, configValue, failReason);
  }

  /**
   * Returns the configuration for this file.
   *
   * @return the configuration for this file
   */
  public FileConfiguration getConfig() {
    return config;
  }

  /**
   * Reloads the configuration file and clears the cache.
   *
   * @return a message indicating that the file was reloaded
   */
  public String reload() {
    clearCache();
    load();
    return "Reloaded " + getName() + EXTENSION;
  }

  /**
   * Saves the configuration to disk.
   *
   * <p>This method will use the last loaded configuration and any changes made to it since then.
   */
  public void saveConfig() {
    YAML.getInstance().saveConfig(getMain(), getConfig(), getName());
  }

  /**
   * Saves the value of the given {@link ConfigValue} to the configuration file.
   *
   * @param configValue the {@link ConfigValue} to save
   */
  public <T> void saveValue(ConfigValue<T> configValue) {
    saveValue(configValue, true);
  }

  /**
   * Saves the value of the given {@link ConfigValue} to the configuration file.
   *
   * @param configValue the {@link ConfigValue} to save
   * @param saveToFile whether to save the value to file
   */
  public <T> void saveValue(ConfigValue<T> configValue, boolean saveToFile) {
    // Validate before saving if validator is set
    ConfigValidator<T> validator = configValue.getValidator();
    if (validator != null) {
      ConfigValidator.ValidationResult result = validator.validate(configValue.getValue());
      if (!result.isValid()) {
        throw new IllegalArgumentException(
            "Cannot save invalid value for '" + configValue.getId() + "': " + result.getErrorMessage());
      }
    }

    saveValue(configValue.getId(), configValue.getValue(), saveToFile);
  }

  /**
   * Sets the value at the given path in the config to the given value.
   *
   * <p>If {@code saveToFile} is true, the value is saved to the configuration file.
   *
   * @param path the path to the value
   * @param value the value to set
   * @param saveToFile whether to save the value to file
   */
  public void saveValue(String path, Object value, boolean saveToFile) {
    getConfig().set(path, value);

    // Update cache
    if (cacheEnabled) {
      valueCache.put(path, value);
    }

    if (saveToFile) {
      YAML.getInstance().saveConfig(getMain(), getConfig(), getName());
    }
  }

  /**
   * Sets the value at the given path in the config to the given value. The value is saved to file.
   *
   * @param path the path to set the value at
   * @param value the value to set at the given path
   */
  public void saveValue(String path, Object value) {
    saveValue(path, value, true);
  }

  private void loadConfig(String configName) {
    File configFile = new File(getMain().getDataFolder(), configName + EXTENSION);
    boolean shouldGenerate = !configFile.exists();

    this.config = YAML.getInstance().getConfig(getMain(), configName);
    clearCache(); // Clear cache when loading new config

    // Clear tracked values before loading
    trackedValues.clear();
    trackedContainers.clear();

    loadData();

    // Auto-generate config file if it didn't exist before OR if it exists but is essentially empty
    // Check if config has no keys (meaning it was just created or is empty)
    if (shouldGenerate || config.getKeys(true).isEmpty()) {
      generateConfigFile(configFile);
    }
  }

  /**
   * SHOULD ALWAYS BE RAN ON PLUGIN ENABLE
   *
   * <p>Loads the configuration file from the given name. Updates any data in memory to the new
   * values in the config file.
   *
   * <p>This method calls {@link #loadConfig(String)} with the name returned by {@link #getName()}.
   */
  public void load() {
    loadConfig(getName());
  }

  /**
   * Annotation for marking fields as config containers.
   * Containers annotated with this will be automatically discovered and loaded by
   * {@link #loadAllContainers()}.
   *
   * <p>Example usage:
   * <pre>{@code
   * @ConfigContainer("debug")
   * private DebugContainer debug;
   *
   * @ConfigContainer("messages")
   * private MessagesContainer messages;
   * }</pre>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  protected @interface ConfigContainer {
    /**
     * The path prefix for this container in the YAML file.
     * For example, "debug" will prefix all values in the container with "debug."
     *
     * @return the path prefix
     */
    String value();
  }

  /**
   * Automatically discovers and loads all fields annotated with {@link ConfigContainer}.
   *
   * <p>This method uses reflection to find all fields with the @ConfigContainer annotation,
   * instantiates them, and calls their load() method.
   *
   * <p>Call this in your {@link #loadData()} implementation:
   * <pre>{@code
   * @Override
   * public void loadData() {
   *     loadAllContainers();
   * }
   * }</pre>
   */
  protected void loadAllContainers() {
    for (Field field : this.getClass().getDeclaredFields()) {
      if (field.isAnnotationPresent(ConfigContainer.class)) {
        field.setAccessible(true);
        ConfigContainer annotation = field.getAnnotation(ConfigContainer.class);
        String pathPrefix = annotation.value();

        try {
          // Instantiate the container
          Class<?> containerClass = field.getType();
          Constructor<?> constructor = containerClass.getConstructor(
              AbstractConfig.class, String.class
          );
          ConfigValueContainer container = (ConfigValueContainer) constructor.newInstance(
              this, pathPrefix
          );

          // Load and assign
          container.load();
          field.set(this, container);

          // Track container for auto-generation
          trackedContainers.put(pathPrefix, container);

        } catch (Exception e) {
          getMain().getLogger().severe(
              "Failed to load container '" + field.getName() + "' at path '" + pathPrefix + "': " + e.getMessage()
          );
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Helper method to manually load a container.
   * This is an alternative to using {@link ConfigContainer} annotation and {@link #loadAllContainers()}.
   *
   * <p>Usage:
   * <pre>{@code
   * debug = loadContainer(new DebugContainer(this, "debug"));
   * }</pre>
   *
   * @param container the container to load
   * @param <T> the type of the container
   * @return the loaded container
   */
  protected <T extends ConfigValueContainer> T loadContainer(T container) {
    container.load();
    return container;
  }

  /**
   * Sets a file header comment that will appear at the top of the generated config file.
   * Call this in your config's constructor or loadData() method.
   *
   * @param fileHeader the multi-line header comment (each line will be prefixed with #)
   */
  protected void setFileHeader(String fileHeader) {
    this.fileHeader = fileHeader;
  }

  /**
   * Gets the file header for this config.
   *
   * @return the file header, or null if none is set
   */
  public String getFileHeader() {
    return fileHeader;
  }

  /**
   * Manually regenerates the config file with current values and comments.
   * This will overwrite the existing file.
   */
  public void regenerateConfigFile() {
    File configFile = new File(getMain().getDataFolder(), getName() + EXTENSION);
    generateConfigFile(configFile);
  }

  /**
   * Generates a config file with all tracked values and their comments.
   *
   * @param configFile the file to write to
   */
  private void generateConfigFile(File configFile) {
    try {
      List<YamlWriter.YamlEntry> entries = new ArrayList<>();

      // Group values by container
      Map<String, List<ConfigValue<?>>> valuesByContainer = new LinkedHashMap<>();

      for (ConfigValue<?> value : trackedValues.values()) {
        String containerKey = getContainerKey(value.getId());
        valuesByContainer.computeIfAbsent(containerKey, k -> new ArrayList<>()).add(value);
      }

      // Create entries with header comments from containers
      for (Map.Entry<String, List<ConfigValue<?>>> entry : valuesByContainer.entrySet()) {
        String containerKey = entry.getKey();
        List<ConfigValue<?>> values = entry.getValue();

        // Get header comment from container if available
        String headerComment = null;
        ConfigValueContainer container = trackedContainers.get(containerKey);
        if (container != null) {
          headerComment = container.getHeaderComment();
        }

        // Current values; the first one in a container carries its header comment
        for (int i = 0; i < values.size(); i++) {
          ConfigValue<?> value = values.get(i);
          entries.add(new YamlWriter.YamlEntry(
              value.getId(),
              value.getValue(),
              value.getComment(),
              i == 0 ? headerComment : null
          ));
        }
      }

      // Write to file with optional file header
      YamlWriter.writeYaml(configFile, entries, fileHeader);

      // Reload the config to pick up the generated values
      this.config = YAML.getInstance().getConfig(getMain(), getName());

      getMain().getLogger().info("Generated config file: " + configFile.getName());

    } catch (IOException | RuntimeException e) {
      logger().log(
          java.util.logging.Level.SEVERE,
          "Failed to generate config file: " + configFile.getName(),
          e);
    }
  }

  /**
   * Gets the container key from a config path.
   * For example, "game.length" returns "game", "simple" returns "".
   */
  private String getContainerKey(String path) {
    int dotIndex = path.indexOf('.');
    if (dotIndex == -1) {
      return "";
    }
    return path.substring(0, dotIndex);
  }

}
