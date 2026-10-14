package me.mortaldev.config2;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a plugin's configurations: registration, load order, loading and reloading.
 *
 * <p><b>Usage in a plugin:</b>
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
 *
 * <p><b>Without the shared instance</b> (tests, or several managers):
 * <pre>{@code
 * ConfigManager manager = ConfigManager.create(logger, path -> ConfigSource.inMemory(null))
 *     .register(new AbilitiesConfigSchema());
 * Map<String, Exception> failures = manager.loadAll();
 * }</pre>
 */
public class ConfigManager {
  private static final List<Class<? extends ConfigSchema>> pending = new ArrayList<>();
  private static ConfigManager instance;

  private final Logger logger;
  private final Function<String, ConfigSource> sources;
  private final List<Registration> registrations = new ArrayList<>();
  private final Map<String, Config> configs = new ConcurrentHashMap<>();
  private final Map<String, Exception> failures = new ConcurrentHashMap<>();

  private ConfigManager(Logger logger, Function<String, ConfigSource> sources) {
    this.logger = logger;
    this.sources = sources;
  }

  /**
   * Creates a manager that stores configs under the plugin's data folder.
   */
  public static ConfigManager forPlugin(JavaPlugin plugin) {
    return create(
        plugin.getLogger(), path -> ConfigSource.file(new File(plugin.getDataFolder(), path)));
  }

  /**
   * Creates a manager with its own logger and storage.
   *
   * @param sources maps a relative path such as {@code "abilities.yml"} to its source
   */
  public static ConfigManager create(Logger logger, Function<String, ConfigSource> sources) {
    return new ConfigManager(logger, sources);
  }

  // ===== Shared instance =====

  /**
   * Initializes the shared ConfigManager with a plugin instance and registers every schema
   * passed to {@link ConfigRegistry#register(Class)} so far.
   * Must be called once in onEnable() before using getInstance().
   *
   * @param plugin the plugin instance
   * @throws IllegalStateException if already initialized
   * @throws IllegalArgumentException if a registered schema cannot be created
   */
  public static synchronized void init(JavaPlugin plugin) {
    if (instance != null) {
      throw new IllegalStateException("ConfigManager already initialized");
    }
    ConfigManager manager = forPlugin(plugin);
    if (pending.isEmpty()) {
      manager.logger.warning(
          "No config schemas registered! Use ConfigRegistry.register() in a static block.");
    }
    for (Class<? extends ConfigSchema> schemaClass : pending) {
      manager.register(schemaClass);
    }
    instance = manager;
  }

  /**
   * Gets the shared ConfigManager instance.
   *
   * @return the ConfigManager instance
   * @throws IllegalStateException if not initialized (call init() first)
   */
  public static synchronized ConfigManager getInstance() {
    if (instance == null) {
      throw new IllegalStateException(
          "ConfigManager not initialized. Call ConfigManager.init(plugin) first.");
    }
    return instance;
  }

  /**
   * Resets the shared instance (for testing). Registered schema classes are kept; clear them
   * with {@link ConfigRegistry#clear()}.
   */
  public static synchronized void reset() {
    instance = null;
  }

  /** Backs {@link ConfigRegistry#register(Class)}. */
  static synchronized void registerShared(Class<? extends ConfigSchema> schemaClass) {
    requireAnnotation(schemaClass);
    pending.add(schemaClass);
    if (instance != null) {
      instance.register(schemaClass);
    }
  }

  static synchronized void clearShared() {
    pending.clear();
  }

  static synchronized int countShared() {
    return pending.size();
  }

  // ===== Registration =====

  /**
   * Registers a schema class annotated with {@link RegisterConfig}.
   *
   * @throws IllegalArgumentException if the class is not annotated, cannot be created, or its
   *     name is already registered
   */
  public ConfigManager register(Class<? extends ConfigSchema> schemaClass) {
    requireAnnotation(schemaClass);
    ConfigSchema schema;
    try {
      schema = schemaClass.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      throw new IllegalArgumentException(
          "Cannot create schema " + schemaClass.getName()
              + " (it needs a public no-argument constructor): " + cause,
          cause);
    }
    return register(schema);
  }

  /**
   * Registers a schema instance. Name, file path and priority come from its class's
   * {@link RegisterConfig} annotation when present, otherwise from {@code schema.name()},
   * {@code name + ".yml"} and priority 100.
   *
   * @throws IllegalArgumentException if the name is already registered
   */
  public synchronized ConfigManager register(ConfigSchema schema) {
    RegisterConfig annotation = schema.getClass().getAnnotation(RegisterConfig.class);
    String name =
        annotation != null && !annotation.value().isEmpty() ? annotation.value() : schema.name();
    String path =
        annotation != null && !annotation.path().isEmpty() ? annotation.path() : name + ".yml";
    int priority = annotation != null ? annotation.priority() : 100;

    if (find(name) != null) {
      throw new IllegalArgumentException("A config named '" + name + "' is already registered");
    }

    ConfigLoader loader =
        ConfigLoader.builder().schema(schema).source(sources.apply(path)).logger(logger).build();
    registrations.add(new Registration(name, path, priority, loader));
    // Stable sort: equal priorities keep registration order
    registrations.sort(Comparator.comparingInt(Registration::priority));
    logger.info("  Registered config: " + name + " at " + path + " (priority: " + priority + ")");
    return this;
  }

  private static void requireAnnotation(Class<? extends ConfigSchema> schemaClass) {
    if (!schemaClass.isAnnotationPresent(RegisterConfig.class)) {
      throw new IllegalArgumentException(
          "Schema class " + schemaClass.getName() + " must be annotated with @RegisterConfig");
    }
  }

  // ===== Loading =====

  /**
   * Loads all registered configurations in priority order (lower first).
   *
   * <p>Auto-generates missing files and validates all values. A config that fails to load is
   * logged and skipped; the others still load.
   *
   * @return the configs that failed, by name, with the reason; empty if all loaded
   */
  public Map<String, Exception> loadAll() {
    logger.info("Loading all configurations...");
    Map<String, Exception> result = loadEach("Loaded");
    logger.info("Loaded " + configs.size() + " configurations");
    return result;
  }

  /**
   * Reloads all configurations. A config that fails keeps its previously loaded values.
   *
   * @return the configs that failed, by name, with the reason; empty if all reloaded
   */
  public Map<String, Exception> reloadAll() {
    logger.info("Reloading all configurations...");
    Map<String, Exception> result = loadEach("Reloaded");
    logger.info("Reloaded " + (registrations.size() - result.size()) + " configurations");
    return result;
  }

  private Map<String, Exception> loadEach(String verb) {
    Map<String, Exception> result = new LinkedHashMap<>();
    for (Registration registration : snapshot()) {
      try {
        load(registration);
        logger.info("  " + verb + ": " + registration.path());
      } catch (RuntimeException e) {
        result.put(registration.name(), e);
      }
    }
    return result;
  }

  /**
   * Reloads a specific configuration by name.
   *
   * @param configName the name of the config to reload
   * @throws IllegalArgumentException if the config doesn't exist
   * @throws ConfigLoader.ConfigLoadException if the config fails validation; the previously
   *     loaded values are kept
   */
  public void reload(String configName) {
    Registration registration = find(configName);
    if (registration == null) {
      throw new IllegalArgumentException("Unknown config: " + configName);
    }
    load(registration);
    logger.info("Reloaded " + registration.path());
  }

  private void load(Registration registration) {
    try {
      configs.put(registration.name(), registration.loader().load());
      failures.remove(registration.name());
    } catch (RuntimeException e) {
      failures.put(registration.name(), e);
      logger.log(Level.SEVERE, "Failed to load config " + registration.name(), e);
      throw e;
    }
  }

  // ===== Access =====

  /**
   * Gets a configuration by name.
   *
   * @param name the config name (e.g., "abilities")
   * @return the config instance
   * @throws IllegalArgumentException if no config with that name is registered
   * @throws IllegalStateException if the config is registered but failed to load
   */
  public Config get(String name) {
    Config config = configs.get(name);
    if (config != null) {
      return config;
    }
    Exception failure = failures.get(name);
    if (failure != null) {
      throw new IllegalStateException(
          "Config '" + name + "' failed to load: " + failure.getMessage(), failure);
    }
    if (find(name) != null) {
      throw new IllegalStateException("Config '" + name + "' is registered but not loaded yet");
    }
    throw new IllegalArgumentException("Unknown config: " + name);
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
   * Validates all loaded configurations.
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

  private synchronized List<Registration> snapshot() {
    return List.copyOf(registrations);
  }

  private synchronized Registration find(String name) {
    for (Registration registration : registrations) {
      if (registration.name().equals(name)) {
        return registration;
      }
    }
    return null;
  }

  private record Registration(String name, String path, int priority, ConfigLoader loader) {}
}
