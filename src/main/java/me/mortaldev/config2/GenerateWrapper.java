package me.mortaldev.config2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a type-safe wrapper class for accessing config values.
 *
 * <p>When applied to a ConfigSchema, an annotation processor will automatically
 * generate a companion wrapper class with static methods for type-safe access.
 *
 * <p><b>Example:</b>
 * <pre>{@code
 * @RegisterConfig(priority = 10)
 * @GenerateWrapper  // ← Add this
 * public class AbilitiesConfigSchema extends ConfigSchema {
 *     public AbilitiesConfigSchema() {
 *         super("abilities");
 *
 *         section("keraunos")
 *             .intValue("cooldown", 10, "Cooldown in seconds")
 *             .doubleValue("radius", 3.0, "Effect radius");
 *     }
 * }
 * }</pre>
 *
 * <p><b>Generates:</b>
 * <pre>{@code
 * public class Abilities {
 *     public static class Keraunos {
 *         public static int cooldown() { ... }
 *         public static double radius() { ... }
 *     }
 * }
 * }</pre>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * int cooldown = Abilities.Keraunos.cooldown();
 * double radius = Abilities.Keraunos.radius();
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GenerateWrapper {

  /**
   * The name of the generated wrapper class.
   *
   * <p>If not specified, removes "Schema" or "ConfigSchema" suffix from the schema class name.
   *
   * <p>Examples:
   * <ul>
   *   <li>AbilitiesConfigSchema → Abilities</li>
   *   <li>CTFSchema → CTF</li>
   *   <li>PhaseConfigSchema → Phase</li>
   * </ul>
   */
  String value() default "";

  /**
   * The package for the generated wrapper class.
   *
   * <p>If not specified, uses the same package as the schema class.
   */
  String packageName() default "";

  /**
   * Whether to generate reload methods.
   *
   * <p>If true, adds static reload() methods to clear cached sections.
   *
   * <p>Default: true
   */
  boolean generateReload() default true;

  /**
   * Whether to make the wrapper class final.
   *
   * <p>Default: true (recommended to prevent inheritance)
   */
  boolean makeFinal() default true;
}