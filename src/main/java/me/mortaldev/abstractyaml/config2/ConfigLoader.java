package me.mortaldev.abstractyaml.config2;

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
 * Loads one config file for a {@link ConfigSchema} and returns it as a {@link Config}.
 *
 * <p>Most plugins don't use this class directly: {@link ConfigManager} creates a loader for every
 * registered schema. Use it to load a single file yourself, or in tests:
 *
 * <pre>{@code
 * Config config = ConfigLoader.builder(plugin)
 *     .schema(new AbilitiesConfigSchema())
 *     .load();                                   // plugins/YourPlugin/abilities.yml
 *
 * Config inTest = ConfigLoader.builder()
 *     .schema(new AbilitiesConfigSchema())
 *     .source(ConfigSource.inMemory("fireball:\n  cooldown: 20\n"))
 *     .load();
 * }</pre>
 *
 * <h2>What {@link #load()} does</h2>
 *
 * <ul>
 *   <li><b>No file yet:</b> every value gets its default and the defaults are written to the file
 *       (unless {@link Builder#autoGenerate(boolean) autoGenerate} is off).
 *   <li><b>YAML syntax error:</b> logged as an error; every value gets its default. The file is
 *       not changed, so the admin can fix it.
 *   <li><b>Path missing from the file:</b> logged as a warning; that value gets its default.
 *   <li><b>Data of the wrong type</b> ({@code cooldown: abc} for an int): logged as a warning;
 *       that value gets its default. See {@link ConfigValue} for what each type accepts.
 *   <li><b>Value fails its validator:</b> {@link ConfigLoadException}, unless
 *       {@link Builder#validateOnLoad(boolean) validateOnLoad} is off. No config is returned.
 *   <li><b>Keys the schema does not declare:</b> ignored and left in the file.
 * </ul>
 *
 * <p>Apart from creating a missing file, loading never writes to the file: missing or invalid
 * values are replaced in memory only.
 *
 * <p>A loader keeps no loaded values; each {@link #load()} reads the source again. It is not
 * synchronized, so don't load the same loader from several threads at once.
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
   * Reads the source and returns the config. See the class description for how missing,
   * mistyped and invalid values are handled.
   *
   * @return the loaded configuration
   * @throws ConfigLoadException if validation is on and a value fails its validator. When the
   *     file does not exist yet, this means a default fails its own validator, and the file is
   *     not created.
   * @throws java.io.UncheckedIOException if the file exists but cannot be read, or cannot be
   *     created
   * @throws IllegalArgumentException if the file must be created and a default list or map
   *     contains something other than String, Number, Boolean, List or Map
   * @throws IllegalStateException if the file must be created and one path is declared both as
   *     a value and as a section
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
   * Loads the file again. Same as {@link #load()}: the source is read fresh and a new config is
   * returned. Configs returned earlier are not changed.
   *
   * @return the newly loaded configuration
   * @throws ConfigLoadException if validation is on and a value fails its validator
   * @throws java.io.UncheckedIOException if the file cannot be read or created
   */
  public Config reload() {
    return load();
  }

  /**
   * Returns where this loader reads and writes the YAML text.
   *
   * @return the source
   */
  public ConfigSource source() {
    return source;
  }

  // ===== Builder =====

  /**
   * Starts a loader for a plugin. Unless you set a {@link Builder#source(ConfigSource) source}
   * or {@link Builder#file(File) file}, the file is {@code <schema name>.yml} in the plugin's
   * data folder, and messages go to the plugin's logger.
   *
   * @param plugin the plugin that owns the file
   * @return a new builder
   */
  public static Builder builder(JavaPlugin plugin) {
    return new Builder(plugin);
  }

  /**
   * Starts a loader without a plugin, for tests or tools that run outside a server. Set a
   * {@link Builder#source(ConfigSource) source} or an absolute {@link Builder#file(File) file};
   * {@link Builder#file(String)} needs a plugin and throws here. Messages go to
   * {@code Logger.getLogger(<schema name>)} unless you set a {@link Builder#logger(Logger)
   * logger}.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder(null);
  }

  /**
   * Configures a {@link ConfigLoader}. Get one from {@link ConfigLoader#builder(JavaPlugin)} or
   * {@link ConfigLoader#builder()}. Only {@link #schema(ConfigSchema)} is required. Each setter
   * returns this builder; setting the same option twice keeps the last value.
   */
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
     * Sets the schema that declares the file's values. Required.
     *
     * @param schema the schema
     * @return this builder
     */
    public Builder schema(ConfigSchema schema) {
      this.schema = schema;
      return this;
    }

    /**
     * Sets where the YAML text is read from and written to, such as
     * {@link ConfigSource#inMemory(String)} in a test. Replaces any earlier
     * {@link #file(File) file}.
     *
     * @param source the source
     * @return this builder
     */
    public Builder source(ConfigSource source) {
      this.source = source;
      return this;
    }

    /**
     * Sets the file to read and write. If neither this nor {@link #source(ConfigSource)} is
     * set, the file is {@code <plugin data folder>/<schema name>.yml}.
     *
     * @param file the file; a relative path is resolved against the server's working
     *     directory, not the plugin's data folder
     * @return this builder
     */
    public Builder file(File file) {
      return source(ConfigSource.file(file));
    }

    /**
     * Sets the file by name, inside the plugin's data folder. {@code ".yml"} is added if the
     * name does not end with it, and the name may include folders:
     * {@code file("arenas/castle")} uses {@code plugins/YourPlugin/arenas/castle.yml}.
     *
     * @param name the file name, relative to the plugin's data folder
     * @return this builder
     * @throws IllegalStateException if the builder came from {@link ConfigLoader#builder()},
     *     which has no plugin
     */
    public Builder file(String name) {
      if (!name.endsWith(".yml")) {
        name = name + ".yml";
      }
      return file(new File(requirePlugin("file(String)").getDataFolder(), name));
    }

    /**
     * Sets whether a missing file is created with the defaults. When off, a missing file still
     * loads with the defaults, but nothing is written.
     *
     * @param autoGenerate {@code true} to create missing files (the default)
     * @return this builder
     */
    public Builder autoGenerate(boolean autoGenerate) {
      this.autoGenerate = autoGenerate;
      return this;
    }

    /**
     * Sets whether values are checked with their validators when loading. When on, a failing
     * value makes {@link ConfigLoader#load()} throw {@link ConfigLoadException}. When off,
     * failing values are loaded as they are; {@link Config#validate()} still reports them.
     *
     * @param validateOnLoad {@code true} to validate (the default)
     * @return this builder
     */
    public Builder validateOnLoad(boolean validateOnLoad) {
      this.validateOnLoad = validateOnLoad;
      return this;
    }

    /**
     * Sets where load messages (file created, missing and invalid values) are logged.
     *
     * @param logger the logger; if not set, the plugin's logger, or
     *     {@code Logger.getLogger(<schema name>)} without a plugin
     * @return this builder
     */
    public Builder logger(Logger logger) {
      this.logger = logger;
      return this;
    }

    /**
     * Creates the loader without loading anything. Call {@link ConfigLoader#load()} on it, as
     * often as needed.
     *
     * @return the loader
     * @throws IllegalStateException if no schema is set, or no source or file is set and the
     *     builder has no plugin to find the default file
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
     * Creates the loader and loads the config once. Same as {@code build().load()}; use
     * {@link #build()} instead if you will reload later.
     *
     * @return the loaded configuration
     * @throws IllegalStateException if the builder is incomplete; see {@link #build()}
     * @throws ConfigLoadException if a value fails validation; see {@link ConfigLoader#load()}
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
   * Thrown when a config cannot be loaded because values fail their validators. The message
   * names the file and lists every failing value on its own line, as {@code "path: message"}.
   *
   * <p>{@link ConfigManager#loadAll()} catches it and returns it in its failure map.
   */
  public static class ConfigLoadException extends RuntimeException {

    /**
     * Creates the exception with a message.
     *
     * @param message what failed
     */
    public ConfigLoadException(String message) {
      super(message);
    }

    /**
     * Creates the exception with a message and the exception that caused it.
     *
     * @param message what failed
     * @param cause the underlying exception
     */
    public ConfigLoadException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
