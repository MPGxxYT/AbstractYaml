package me.mortaldev.config2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a ConfigSchema class for automatic registration with ConfigManager.
 *
 * <p>Example usage:
 * <pre>{@code
 * @RegisterConfig(priority = 10)
 * public class AbilitiesConfigSchema extends ConfigSchema {
 *     public AbilitiesConfigSchema() {
 *         super("abilities");
 *         // ... define values
 *     }
 * }
 * }</pre>
 *
 * <p>Then register in your Main class:
 * <pre>{@code
 * static {
 *     ConfigRegistry.register(AbilitiesConfigSchema.class);
 *     ConfigRegistry.register(CTFConfigSchema.class);
 * }
 *
 * public void onEnable() {
 *     ConfigManager.init(this);
 *     ConfigManager.getInstance().loadAll();
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterConfig {

  /**
   * Priority for loading order (lower numbers load first).
   *
   * <p>Use this when configs depend on each other.
   * For example, if config B needs values from config A,
   * give config A a lower priority (e.g., 10) and config B
   * a higher priority (e.g., 20).
   *
   * <p>Default is 100.
   */
  int priority() default 100;

  /**
   * Optional: Override the config name.
   *
   * <p>By default, uses the name from the schema's constructor.
   * You can override it here if needed.
   *
   * <p>Example: {@code @RegisterConfig(value = "custom-name")}
   */
  String value() default "";

  /**
   * Optional: Specify a custom file path relative to the plugin data folder.
   *
   * <p>By default, configs are saved to the root data folder as "{name}.yml".
   * Use this to organize configs into subdirectories.
   *
   * <p>Examples:
   * <ul>
   *   <li>{@code path = "configs/abilities.yml"} - saves to plugins/YourPlugin/configs/abilities.yml</li>
   *   <li>{@code path = "settings/game.yml"} - saves to plugins/YourPlugin/settings/game.yml</li>
   *   <li>{@code path = ""} (default) - saves to plugins/YourPlugin/{name}.yml</li>
   * </ul>
   */
  String path() default "";
}
