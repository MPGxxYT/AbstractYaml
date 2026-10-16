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
 * <p>Classes registered after {@code init} are added to the running manager immediately.
 * Code that builds its own manager can call {@link ConfigManager#register(Class)} instead.
 */
public final class ConfigRegistry {

  private ConfigRegistry() {}

  /**
   * Registers a ConfigSchema class for automatic loading.
   *
   * <p>The schema class must be annotated with {@link RegisterConfig}.
   *
   * @param schemaClass the schema class to register
   * @throws IllegalArgumentException if the class is not annotated with @RegisterConfig
   */
  public static void register(Class<? extends ConfigSchema> schemaClass) {
    ConfigManager.registerShared(schemaClass);
  }

  /**
   * Clears all registered schemas.
   * Useful for testing.
   */
  public static void clear() {
    ConfigManager.clearShared();
  }

  /**
   * Gets the number of registered schemas.
   *
   * @return count of registered schemas
   */
  public static int count() {
    return ConfigManager.countShared();
  }
}
