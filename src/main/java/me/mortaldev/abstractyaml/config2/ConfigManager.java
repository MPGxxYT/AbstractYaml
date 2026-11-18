package me.mortaldev.abstractyaml.config2;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a plugin's configs: which schemas exist, the order they load in, and the loaded
 * {@link Config} for each.
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
 *     Map<String, Exception> failures = ConfigManager.getInstance().loadAll();
 *     if (!failures.isEmpty()) {
 *         getLogger().severe("Some configs failed to load: " + failures.keySet());
 *     }
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
 *
 * <p>Each config has a name (used with {@link #get(String)}), a file path relative to the
 * plugin's data folder, and a priority. All three come from {@link RegisterConfig}; without the
 * annotation they are the schema's name, {@code <name>.yml} and 100.
 *
 * <p>{@link #get(String)} and the other read methods can be called from any thread, including
 * async tasks. Load and reload from one thread at a time, normally the main server thread.
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
   * Creates a manager that stores configs in the plugin's data folder and logs to the plugin's
   * logger. This does not make it the shared instance; for that, use {@link #init(JavaPlugin)}.
   *
   * @param plugin the plugin that owns the files
   * @return a new manager with no schemas registered
   */
  public static ConfigManager forPlugin(JavaPlugin plugin) {
    return create(
        plugin.getLogger(), path -> ConfigSource.file(new File(plugin.getDataFolder(), path)));
  }

  /**
   * Creates a manager with its own logger and storage. In tests, pass
   * {@code path -> ConfigSource.inMemory(null)} so nothing touches the disk.
   *
   * @param logger where load messages are logged
   * @param sources maps a relative path such as {@code "abilities.yml"} to its source; called
   *     once per schema when it is registered
   * @return a new manager with no schemas registered
   */
  public static ConfigManager create(Logger logger, Function<String, ConfigSource> sources) {
    return new ConfigManager(logger, sources);
  }

  // ===== Shared instance =====

  /**
   * Creates the shared manager for a plugin and registers every schema class passed to
   * {@link ConfigRegistry#register(Class)} so far. Call it once in {@code onEnable()}, then call
   * {@link #loadAll()}; {@code init} does not load anything.
   *
   * <p>Logs a warning if no schemas are registered.
   *
   * @param plugin the plugin instance
   * @throws IllegalStateException if already initialized; call {@link #reset()} first, for
   *     example in {@code onDisable()} so a {@code /reload} can initialize again
   * @throws IllegalArgumentException if a registered schema cannot be created, or two schemas
   *     have the same name
   */
  public static synchronized void init(JavaPlugin plugin) {
    if (instance != null) {
      throw new IllegalStateException("ConfigManager already initialized");
    }
    init(forPlugin(plugin));
  }

  /**
   * Makes {@code manager} the shared instance and registers every schema class passed to
   * {@link ConfigRegistry#register(Class)} that it does not already have. Use this in tests, or
   * to choose where the shared manager stores its files. Does not load anything.
   *
   * @param manager the manager to share, usually from {@link #create(Logger, Function)}
   * @throws IllegalStateException if already initialized
   * @throws IllegalArgumentException if a registered schema cannot be created, or two schemas
   *     have the same name
   */
  public static synchronized void init(ConfigManager manager) {
    if (instance != null) {
      throw new IllegalStateException("ConfigManager already initialized");
    }
    if (pending.isEmpty() && manager.snapshot().isEmpty()) {
      manager.logger.warning(
          "No config schemas registered! Use ConfigRegistry.register() in a static block.");
    }
    for (Class<? extends ConfigSchema> schemaClass : pending) {
      if (!manager.hasSchemaClass(schemaClass)) {
        manager.register(schemaClass);
      }
    }
    instance = manager;
  }

  /**
   * Returns the shared manager. Generated wrappers ({@link GenerateWrapper}) call this on every
   * accessor.
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
   * Removes the shared instance, so {@link #getInstance()} throws until the next
   * {@code init}. Call it in tests between cases, and in {@code onDisable()} if the plugin can
   * be enabled again in the same JVM. Schema classes passed to {@link ConfigRegistry#register}
   * are kept and registered again by the next {@code init}; clear them with
   * {@link ConfigRegistry#clear()}.
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
   * Creates a schema from its class and registers it, as {@link #register(ConfigSchema)} does.
   * The class must be public, annotated with {@link RegisterConfig}, and have a public
   * no-argument constructor. A nested schema class must be {@code static}.
   *
   * <p>Registering does not load the config. If the manager has already loaded, call
   * {@link #reload(String)} or {@link #loadAll()} to load the new one.
   *
   * @param schemaClass the schema class
   * @return this manager, for chaining
   * @throws IllegalArgumentException if the class is not annotated, cannot be created, or its
   *     name is already registered. If the schema's constructor throws, that exception is the
   *     cause.
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
   * {@code name + ".yml"} and priority 100. The annotation is optional here.
   *
   * <p>Registering does not load the config. If the manager has already loaded, call
   * {@link #reload(String)} or {@link #loadAll()} to load the new one.
   *
   * @param schema the schema
   * @return this manager, for chaining
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
    registrations.add(new Registration(name, path, priority, loader, schema.getClass()));
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
   * Loads every registered config in priority order (lower first; equal priorities in
   * registration order). Missing files are created with their defaults; see
   * {@link ConfigLoader} for how missing and invalid values are handled.
   *
   * <p>A config that fails to load is logged with its stack trace and skipped; the others
   * still load. Until it loads, {@link #get(String)} throws for it. Calling this again loads
   * everything again, the same as {@link #reloadAll()}.
   *
   * @return the configs that failed, by name, with the reason (usually
   *     {@link ConfigLoader.ConfigLoadException}), in load order; empty if all loaded
   */
  public Map<String, Exception> loadAll() {
    logger.info("Loading all configurations...");
    Map<String, Exception> result = loadEach("Loaded");
    logger.info("Loaded " + configs.size() + " configurations");
    return result;
  }

  /**
   * Reads every registered config from its file again, in priority order. A config that fails
   * keeps its previously loaded values, so {@link #get(String)} still returns them; the failure
   * is logged and returned. Configs obtained before the reload are not changed; call
   * {@link #get(String)} again for the new values.
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
   * Reads one config from its file again. Also loads a config registered after
   * {@link #loadAll()}.
   *
   * @param configName the name of the config to reload
   * @throws IllegalArgumentException if the config doesn't exist
   * @throws ConfigLoader.ConfigLoadException if the config fails validation; the previously
   *     loaded values are kept
   * @throws java.io.UncheckedIOException if the file cannot be read or created; the previously
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
   * Returns the loaded config with the given name. The result does not change on reload; call
   * this again afterwards for the new values, rather than keeping the result in a field.
   *
   * @param name the config name (e.g., "abilities"), from {@link RegisterConfig#value()} or the
   *     schema's name
   * @return the config instance
   * @throws IllegalArgumentException if no config with that name is registered
   * @throws IllegalStateException if the config is registered but has never loaded, either
   *     because nothing has been loaded yet or because it failed; the message includes the
   *     failure
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
   * Returns a section of a loaded config. Same as {@code get(configName).section(sectionPath)}.
   *
   * @param configName the config name
   * @param sectionPath the section path (e.g., "keraunos")
   * @return the config section
   * @throws IllegalArgumentException if no config with that name is registered
   * @throws IllegalStateException if the config has not loaded
   */
  public ConfigSection getSection(String configName, String sectionPath) {
    return get(configName).section(sectionPath);
  }

  /**
   * Returns the names of the configs that have loaded at least once. Registered configs that
   * have never loaded are not included.
   *
   * @return an unmodifiable copy of the names
   */
  public Set<String> getConfigNames() {
    return Set.copyOf(configs.keySet());
  }

  /**
   * Checks every loaded config with its validators. Configs that have never loaded are not
   * included.
   *
   * @return each loaded config's name with its result, in no particular order
   */
  public Map<String, ValidationResult> validateAll() {
    Map<String, ValidationResult> results = new HashMap<>();
    for (Map.Entry<String, Config> entry : configs.entrySet()) {
      results.put(entry.getKey(), entry.getValue().validate());
    }
    return results;
  }

  /**
   * Checks whether a config has loaded at least once. A config whose latest reload failed is
   * still loaded, with its earlier values.
   *
   * @param name the config name
   * @return {@code true} if {@link #get(String)} will return a config for this name
   */
  public boolean isLoaded(String name) {
    return configs.containsKey(name);
  }

  /**
   * Returns the number of configs that have loaded at least once.
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

  private synchronized boolean hasSchemaClass(Class<? extends ConfigSchema> schemaClass) {
    for (Registration registration : registrations) {
      if (registration.schemaClass() == schemaClass) {
        return true;
      }
    }
    return false;
  }

  private record Registration(
      String name, String path, int priority, ConfigLoader loader, Class<?> schemaClass) {}
}
