package me.mortaldev.config2;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton manager for all plugin configurations.
 *
 * <p>Automatically discovers and loads all @RegisterConfig annotated schemas.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * // 1. Register schemas in static block
 * static {
 *     ConfigRegistry.register(AbilitiesConfigSchema.class);
 *     ConfigRegistry.register(CTFConfigSchema.class);
 * }
 *
 * // 2. Initialize in onEnable()
 * public void onEnable() {
 *     ConfigManager.init(this);
 *     ConfigManager.getInstance().loadAll();
 * }
 *
 * // 3. Access anywhere
 * Config abilities = ConfigManager.getInstance().get("abilities");
 * int cooldown = abilities.getInt("keraunos.cooldown");
 * }</pre>
 */
public class ConfigManager {
  private static ConfigManager instance;

  private final JavaPlugin plugin;
  private final Map<String, ConfigLoader> loaders = new LinkedHashMap<>();
  private final Map<String, Config> configs = new ConcurrentHashMap<>();

  private ConfigManager(JavaPlugin plugin) {
    this.plugin = plugin;
    discoverAndRegisterSchemas();
  }

  /**
   * Initializes the ConfigManager with a plugin instance.
   * Must be called once in onEnable() before using getInstance().
   *
   * @param plugin the plugin instance
   * @throws IllegalStateException if already initialized
   */
  public static void init(JavaPlugin plugin) {
    if (instance != null) {
      throw new IllegalStateException("ConfigManager already initialized");
    }
    instance = new ConfigManager(plugin);
  }

  /**
   * Gets the ConfigManager singleton instance.
   *
   * @return the ConfigManager instance
   * @throws IllegalStateException if not initialized (call init() first)
   */
  public static ConfigManager getInstance() {
    if (instance == null) {
      throw new IllegalStateException(
          "ConfigManager not initialized. Call ConfigManager.init(plugin) first.");
    }
    return instance;
  }

  /**
   * Resets the ConfigManager (for testing).
   */
  public static void reset() {
    instance = null;
  }

  /**
   * Discovers all @RegisterConfig annotated schemas from the registry
   * and creates loaders for them.
   */
  private void discoverAndRegisterSchemas() {
    plugin.getLogger().info("Discovering config schemas...");

    List<Class<? extends ConfigSchema>> schemaClasses = ConfigRegistry.getRegistered();

    if (schemaClasses.isEmpty()) {
      plugin
          .getLogger()
          .warning(
              "No config schemas registered! Use ConfigRegistry.register() in a static block.");
      return;
    }

    // Sort by priority (lower = earlier)
    List<SchemaEntry> entries = new ArrayList<>();
    for (Class<? extends ConfigSchema> clazz : schemaClasses) {
      RegisterConfig annotation = clazz.getAnnotation(RegisterConfig.class);
      int priority = annotation.priority();
      entries.add(new SchemaEntry(clazz, priority));
    }
    entries.sort(Comparator.comparingInt(e -> e.priority));

    // Instantiate schemas and create loaders
    for (SchemaEntry entry : entries) {
      try {
        ConfigSchema schema = entry.schemaClass.getDeclaredConstructor().newInstance();

        // Use custom name from annotation if provided, otherwise use schema.name()
        RegisterConfig annotation = entry.schemaClass.getAnnotation(RegisterConfig.class);
        String name = annotation.value().isEmpty() ? schema.name() : annotation.value();

        // Build the loader with custom path if specified
        ConfigLoader.Builder builder = ConfigLoader.builder(plugin).schema(schema);

        if (!annotation.path().isEmpty()) {
          // Custom path specified
          java.io.File customFile = new java.io.File(plugin.getDataFolder(), annotation.path());
          builder.file(customFile);
          plugin
              .getLogger()
              .info("  Registered config: " + name + " at " + annotation.path() + " (priority: " + entry.priority + ")");
        } else {
          // Default path (root data folder)
          plugin
              .getLogger()
              .info("  Registered config: " + name + " (priority: " + entry.priority + ")");
        }

        ConfigLoader loader = builder.build();
        loaders.put(name, loader);

      } catch (Exception e) {
        plugin
            .getLogger()
            .severe("Failed to register schema: " + entry.schemaClass.getName());
        e.printStackTrace();
      }
    }

    plugin.getLogger().info("Registered " + loaders.size() + " config schemas");
  }

  /**
   * Loads all registered configurations from disk.
   *
   * <p>Auto-generates missing files and validates all values.
   */
  public void loadAll() {
    plugin.getLogger().info("Loading all configurations...");

    for (Map.Entry<String, ConfigLoader> entry : loaders.entrySet()) {
      String name = entry.getKey();
      ConfigLoader loader = entry.getValue();

      try {
        Config config = loader.load();
        configs.put(name, config);
        plugin.getLogger().info("  Loaded: " + name + ".yml");
      } catch (Exception e) {
        plugin.getLogger().severe("Failed to load config: " + name);
        e.printStackTrace();
      }
    }

    plugin.getLogger().info("Loaded " + configs.size() + " configurations");
  }

  /**
   * Reloads all configurations from disk.
   */
  public void reloadAll() {
    plugin.getLogger().info("Reloading all configurations...");

    for (Map.Entry<String, ConfigLoader> entry : loaders.entrySet()) {
      String name = entry.getKey();
      ConfigLoader loader = entry.getValue();

      try {
        Config config = loader.reload();
        configs.put(name, config);
        plugin.getLogger().info("  Reloaded: " + name + ".yml");
      } catch (Exception e) {
        plugin.getLogger().severe("Failed to reload config: " + name);
        e.printStackTrace();
      }
    }

    plugin.getLogger().info("Reloaded " + configs.size() + " configurations");
  }

  /**
   * Reloads a specific configuration by name.
   *
   * @param configName the name of the config to reload
   * @throws IllegalArgumentException if the config doesn't exist
   */
  public void reload(String configName) {
    ConfigLoader loader = loaders.get(configName);
    if (loader == null) {
      throw new IllegalArgumentException("Unknown config: " + configName);
    }

    Config reloaded = loader.reload();
    configs.put(configName, reloaded);
    plugin.getLogger().info("Reloaded " + configName + ".yml");
  }

  /**
   * Gets a configuration by name.
   *
   * @param name the config name (e.g., "abilities")
   * @return the config instance
   * @throws IllegalArgumentException if the config doesn't exist
   */
  public Config get(String name) {
    Config config = configs.get(name);
    if (config == null) {
      throw new IllegalArgumentException("Unknown config: " + name);
    }
    return config;
  }

  /**
   * Gets a config section directly.
   *
   * @param configName the config name
   * @param sectionPath the section path (e.g., "keraunos")
   * @return the config section
   */
  public ConfigSection getSection(String configName, String sectionPath) {
    return get(configName).section(sectionPath);
  }

  /**
   * Gets all loaded configuration names.
   *
   * @return set of config names
   */
  public Set<String> getConfigNames() {
    return Set.copyOf(configs.keySet());
  }

  /**
   * Validates all configurations.
   *
   * @return map of config names to validation results
   */
  public Map<String, ValidationResult> validateAll() {
    Map<String, ValidationResult> results = new HashMap<>();
    for (Map.Entry<String, Config> entry : configs.entrySet()) {
      results.put(entry.getKey(), entry.getValue().validate());
    }
    return results;
  }

  /**
   * Checks if a config is loaded.
   *
   * @param name the config name
   * @return true if loaded
   */
  public boolean isLoaded(String name) {
    return configs.containsKey(name);
  }

  /**
   * Gets the number of loaded configs.
   *
   * @return count of loaded configs
   */
  public int count() {
    return configs.size();
  }

  /** Internal record for sorting schemas by priority. */
  private record SchemaEntry(Class<? extends ConfigSchema> schemaClass, int priority) {}
}
