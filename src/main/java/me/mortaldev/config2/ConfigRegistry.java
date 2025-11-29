package me.mortaldev.config2;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry for manually registering ConfigSchema classes.
 *
 * <p>This allows you to explicitly register which schemas should be loaded
 * by the ConfigManager without needing classpath scanning or external dependencies.
 *
 * <p>Usage in your Main class:
 * <pre>{@code
 * static {
 *     ConfigRegistry.register(AbilitiesConfigSchema.class);
 *     ConfigRegistry.register(CTFConfigSchema.class);
 *     ConfigRegistry.register(UltimateConfigSchema.class);
 * }
 * }</pre>
 */
public class ConfigRegistry {
  private static final List<Class<? extends ConfigSchema>> schemas = new ArrayList<>();

  /**
   * Registers a ConfigSchema class for automatic loading.
   *
   * <p>The schema class must be annotated with {@link RegisterConfig}.
   *
   * @param schemaClass the schema class to register
   * @throws IllegalArgumentException if the class is not annotated with @RegisterConfig
   */
  public static void register(Class<? extends ConfigSchema> schemaClass) {
    if (!schemaClass.isAnnotationPresent(RegisterConfig.class)) {
      throw new IllegalArgumentException(
          "Schema class " + schemaClass.getName() + " must be annotated with @RegisterConfig");
    }
    schemas.add(schemaClass);
  }

  /**
   * Gets all registered schema classes.
   *
   * @return immutable list of registered schemas
   */
  static List<Class<? extends ConfigSchema>> getRegistered() {
    return List.copyOf(schemas);
  }

  /**
   * Clears all registered schemas.
   * Useful for testing.
   */
  public static void clear() {
    schemas.clear();
  }

  /**
   * Gets the number of registered schemas.
   *
   * @return count of registered schemas
   */
  public static int count() {
    return schemas.size();
  }
}
