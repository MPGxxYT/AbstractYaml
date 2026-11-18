package me.mortaldev.abstractyaml;

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
 * Base class for one legacy config file. Subclass it once per file, keep each value in a
 * {@link ConfigValue} field, and read the values in {@link #loadData()}:
 *
 * <pre>{@code
 * public class MainConfig extends AbstractConfig {
 *     private final JavaPlugin plugin;
 *     private ConfigValue<Integer> cooldown;
 *
 *     public MainConfig(JavaPlugin plugin) {
 *         this.plugin = plugin;
 *     }
 *
 *     @Override public String getName() { return "config"; }      // config.yml
 *     @Override public JavaPlugin getMain() { return plugin; }
 *     @Override public void log(String message) { plugin.getLogger().info(message); }
 *
 *     @Override
 *     public void loadData() {
 *         cooldown = getConfigValue(new ConfigValue<>("cooldown", Integer.class, 10)
 *             .setValidator(ConfigValidator.range(1, 300))
 *             .setComment("Seconds between uses"));
 *     }
 *
 *     public int getCooldown() { return cooldown.getValue(); }
 * }
 *
 * // In onEnable():
 * mainConfig = new MainConfig(this);
 * mainConfig.load();
 * }</pre>
 *
 * <h2>Loading</h2>
 *
 * <p>{@link #load()} reads {@code <getName()>.yml} from the plugin's data folder and calls
 * {@link #loadData()}. For each value, {@link #getConfigValue(ConfigValue)}:
 * <ul>
 *   <li>uses the file's value when it has the expected type and passes the validator;
 *   <li>uses the default, <b>and writes the default to the file at once</b>, when the file's
 *       value has the wrong type or fails the validator (a warning is logged);
 *   <li>uses the default without writing anything when the path is missing from the file.
 * </ul>
 *
 * <p>Numbers convert between {@code Integer}, {@code Long}, {@code Float} and {@code Double}
 * when nothing is lost: {@code 5} works for a {@code Double} value and {@code 5.0} for an
 * {@code Integer} value, but {@code 5.5} for an {@code Integer} value is invalid. Nothing else
 * is converted: {@code name: 5} for a {@code String} value is invalid.
 *
 * <p>If the file did not exist, or has no keys, it is created after {@link #loadData()} from
 * every value that was read, with their comments. When the plugin jar contains a file of the
 * same name, it is copied first, so its values are kept. When it does not, the plugin's logger
 * shows {@code "Failed to load resource: <name>.yml"}; that warning is harmless here.
 *
 * <h2>Caching</h2>
 *
 * <p>Values are cached by path. A second {@link #getConfigValue(ConfigValue)} for the same path
 * returns the cached value without reading the file. {@link #load()} and {@link #reload()}
 * clear the cache.
 *
 * <p>This class is not thread-safe: load and save on the main server thread.
 *
 * @deprecated Part of the legacy config system, kept for existing plugins. New code should use
 *     {@code me.mortaldev.abstractyaml.config2}: a {@code ConfigSchema} subclass registered with
 *     {@code ConfigManager}. See {@code docs/migrating-from-legacy.md}.
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
   * Logs a message. The library does not call this; it is for your own code. Library warnings
   * go to {@code getMain().getLogger()}.
   *
   * @param message the message to log
   */
  public abstract void log(String message);

  /**
   * Returns the name of this configuration file, without the extension. {@code "config"}
   * means {@code config.yml} in the plugin's data folder; {@code "arenas/castle"} means
   * {@code arenas/castle.yml}.
   *
   * @return the name of this configuration file
   */
  public abstract String getName();

  /**
   * Returns the plugin that owns this file. Its data folder contains the file, its jar may
   * contain a default copy, and its logger receives warnings.
   *
   * @return the main plugin; must not be {@code null} when loading or saving
   */
  public abstract JavaPlugin getMain();

  /**
   * Reads every value of this file. Called by {@link #load()} and {@link #reload()} after the
   * file is read. Assign each {@link ConfigValue} field from
   * {@link #getConfigValue(ConfigValue)}, or call {@link #loadAllContainers()}.
   *
   * <p>Only values requested here are written when the file is generated, so request every
   * value on each load, even ones you read rarely.
   */
  public abstract void loadData();

  /** Creates the config. Nothing is read until {@link #load()}. */
  public AbstractConfig() {}

  /**
   * Enables or disables caching for this configuration. With caching off, every
   * {@link #getConfigValue(ConfigValue)} reads the value from the file contents loaded in
   * memory (not from disk). Turning it off also clears the cache.
   *
   * @param enabled true to enable caching (the default), false to disable
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
   * Clears the value cache, so the next {@link #getConfigValue(ConfigValue)} reads from the
   * file contents loaded in memory. To pick up changes made to the file on disk, call
   * {@link #reload()} instead.
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
   * Reads a value from the file into {@code configValue} and returns it. Call this from
   * {@link #loadData()}. See the class description for how wrong-typed, invalid and missing
   * values are handled; an invalid value is replaced by the default <b>in the file</b> too.
   *
   * <p>The given object is changed and returned, so
   * {@code cooldown = getConfigValue(new ConfigValue<>("cooldown", Integer.class, 10))} is the
   * usual form. The value is also remembered for generating the file.
   *
   * @param configValue the value to read, with its path, type, default and optional validator
   *     and comment
   * @return {@code configValue}, now containing the value from the file or the default
   * @param <T> the type of the value
   * @throws IllegalArgumentException if the value is invalid and its default also fails the
   *     validator, so the default cannot be saved
   * @throws YAML.ConfigurationException if the default has to be written and the file cannot
   *     be saved
   * @throws NullPointerException if called before {@link #load()}
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

  /**
   * Logs that a value could not be loaded and, if the plugin jar contains
   * {@code <configName>.yml}, copies it over the file in the data folder, replacing the admin's
   * edits. The loaded values are not changed; call {@link #reload()} to read the restored file.
   *
   * @param configName the file name, without {@code .yml}
   * @param configValue the path or name of the value that failed, for the message
   * @return the logged message: {@code "[<configName>.YML] Failed to load config value:
   *     <configValue> (INVALID VALUE)"}
   * @throws YAML.ConfigurationException if the bundled file cannot be copied
   */
  public String failedToLoad(String configName, String configValue) {
    return YAML.getInstance().failedToLoad(getMain(), configName, configValue);
  }

  /**
   * Same as {@link #failedToLoad(String, String)}, with your own reason in the message.
   *
   * @param configName the file name, without {@code .yml}
   * @param configValue the path or name of the value that failed, for the message
   * @param failReason why it failed, shown in parentheses
   * @return the logged message
   * @throws YAML.ConfigurationException if the bundled file cannot be copied
   */
  public String failedToLoad(String configName, String configValue, String failReason) {
    return YAML.getInstance().failedToLoad(getMain(), configName, configValue, failReason);
  }

  /**
   * Returns Bukkit's in-memory copy of the file, for reading or setting values that have no
   * {@link ConfigValue}. Changes are written by {@link #saveConfig()}.
   *
   * @return the configuration for this file, or {@code null} before {@link #load()}
   */
  public FileConfiguration getConfig() {
    return config;
  }

  /**
   * Reads the file from disk again, clears the cache and calls {@link #loadData()}, so every
   * {@link ConfigValue} field gets the file's current value. Use it for a reload command.
   *
   * @return {@code "Reloaded <name>.yml"}, for showing to the command sender
   */
  public String reload() {
    clearCache();
    load();
    return "Reloaded " + getName() + EXTENSION;
  }

  /**
   * Writes the in-memory configuration ({@link #getConfig()}) to the file, including changes
   * made since it was loaded. Creates the data folder if needed.
   *
   * @throws YAML.ConfigurationException if the file cannot be written
   */
  public void saveConfig() {
    YAML.getInstance().saveConfig(getMain(), getConfig(), getName());
  }

  /**
   * Validates the value of {@code configValue}, stores it at its path and writes the file.
   *
   * <pre>{@code
   * cooldown.setValue(20);
   * saveValue(cooldown);
   * }</pre>
   *
   * @param configValue the {@link ConfigValue} to save
   * @param <T> the type of the value
   * @throws IllegalArgumentException if the value fails its validator; nothing is saved
   * @throws YAML.ConfigurationException if the file cannot be written
   */
  public <T> void saveValue(ConfigValue<T> configValue) {
    saveValue(configValue, true);
  }

  /**
   * Validates the value of {@code configValue} and stores it at its path, writing the file only
   * if {@code saveToFile} is true. Pass {@code false} when saving several values, then call
   * {@link #saveConfig()} once.
   *
   * @param configValue the {@link ConfigValue} to save
   * @param saveToFile whether to save the value to file
   * @param <T> the type of the value
   * @throws IllegalArgumentException if the value fails its validator; nothing is saved
   * @throws YAML.ConfigurationException if the file cannot be written
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
   * Stores a value at a path, without validation, and updates the cache. If {@code saveToFile}
   * is true, the file is written.
   *
   * <p>{@link ConfigValue} fields for this path are not updated; read the value again or
   * call {@link #reload()}. Passing {@code null} removes the path.
   *
   * @param path the path to the value
   * @param value the value to set
   * @param saveToFile whether to save the value to file
   * @throws YAML.ConfigurationException if the file cannot be written
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
   * Stores a value at a path, without validation, and writes the file. Same as
   * {@code saveValue(path, value, true)}.
   *
   * @param path the path to set the value at
   * @param value the value to set at the given path
   * @throws YAML.ConfigurationException if the file cannot be written
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
   * Reads the file and calls {@link #loadData()}. Call it once in {@code onEnable()}, before
   * using any value. See the class description for what happens with a missing file and with
   * invalid values.
   *
   * <p>Unlike {@link #reload()}, this returns nothing; otherwise they do the same work.
   *
   * @throws NullPointerException if {@link #getMain()} returns {@code null}
   */
  public void load() {
    loadConfig(getName());
  }

  /**
   * Marks a {@link ConfigValueContainer} field to be created and loaded by
   * {@link #loadAllContainers()}. The field must be declared in your config class itself (not a
   * superclass), and its type needs a public constructor taking
   * {@code (AbstractConfig, String)}.
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
   * Creates, loads and assigns every field annotated with {@link ConfigContainer} that is
   * declared in this class (fields of superclasses are skipped). Each container gets its
   * annotation value as its path prefix, and its header comment is written above its section
   * when the file is generated.
   *
   * <p>If a container cannot be created (no public {@code (AbstractConfig, String)}
   * constructor, or its {@code load()} throws), the error is logged and that field is left
   * unchanged; the other containers still load.
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
   * Loads a container you created yourself, as an alternative to {@link ConfigContainer} and
   * {@link #loadAllContainers()}. A header comment set by a container loaded this way is not
   * written to a generated file.
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
   * Sets the comment written at the top of the file when it is generated. Call this in your
   * config's constructor or {@link #loadData()}, before the file is generated. Has no effect on
   * an existing file until {@link #regenerateConfigFile()}.
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
   * Rewrites the file from the values read by the last {@link #loadData()}, with their current
   * values, comments and headers. The file is rewritten: keys that no
   * {@link ConfigValue} reads, and comments the admin added, are removed. Errors are logged,
   * not thrown.
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
