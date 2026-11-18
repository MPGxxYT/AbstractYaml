package me.mortaldev.abstractyaml.config2;

/**
 * Registers schema classes with the shared {@link ConfigManager} from a static block, before
 * {@link ConfigManager#init} runs.
 *
 * <p>Usage in your Main class:
 * <pre>{@code
 * static {
 *     ConfigRegistry.register(AbilitiesConfigSchema.class);
 *     ConfigRegistry.register(CTFConfigSchema.class);
 *     ConfigRegistry.register(UltimateConfigSchema.class);
 * }
 * }</pre>
 *
 * <p>Classes registered after {@code init} are added to the running manager immediately, but
 * not loaded; call {@link ConfigManager#reload(String)} for them. Code that builds its own
 * manager can call {@link ConfigManager#register(Class)} instead.
 */
public final class ConfigRegistry {

  private ConfigRegistry() {}

  /**
   * Adds a schema class to the list the shared manager registers on {@link ConfigManager#init}.
   * The class is only checked for the annotation here; it is created when {@code init} runs.
   *
   * <p>The schema class must be public, annotated with {@link RegisterConfig}, and have a public
   * no-argument constructor. Registering the same class twice is harmless: {@code init} skips
   * classes the manager already has.
   *
   * @param schemaClass the schema class to register
   * @throws IllegalArgumentException if the class is not annotated with @RegisterConfig, or, when
   *     called after {@code init}, if it cannot be created or its name is already registered
   */
  public static void register(Class<? extends ConfigSchema> schemaClass) {
    ConfigManager.registerShared(schemaClass);
  }

  /**
   * Removes every class passed to {@link #register(Class)}, so the next
   * {@link ConfigManager#init} registers none of them. A manager that is already initialized
   * keeps its schemas. Use it between tests, together with {@link ConfigManager#reset()}.
   */
  public static void clear() {
    ConfigManager.clearShared();
  }

  /**
   * Returns how many times {@link #register(Class)} has been called since the last
   * {@link #clear()}. A class registered twice counts twice.
   *
   * @return count of registered schemas
   */
  public static int count() {
    return ConfigManager.countShared();
  }
}
