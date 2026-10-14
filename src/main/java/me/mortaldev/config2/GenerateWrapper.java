package me.mortaldev.config2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a class of static, typed accessors for a schema at compile time.
 *
 * <p>The annotation processor shipped in AbstractYaml reads the schema's constructor and writes
 * the wrapper into the build's generated sources. Enable it by adding AbstractYaml to the
 * compiler's annotation processor path (see {@code docs/generated-wrappers.md}).
 *
 * <p><b>Example:</b>
 * <pre>{@code
 * @RegisterConfig(priority = 10)
 * @GenerateWrapper
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
 * <p>generates {@code wrappers.AbilitiesWrapper}, used as:
 * <pre>{@code
 * int cooldown = AbilitiesWrapper.Keraunos.cooldown();
 * double radius = AbilitiesWrapper.Keraunos.radius();
 * }</pre>
 *
 * <p>Accessors read the config loaded by the shared {@link ConfigManager} on each call, so they
 * always reflect the latest reload. If a class with the wrapper's name already exists in the
 * sources (for example a hand-edited copy), it is left alone and nothing is generated.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GenerateWrapper {

  /**
   * The simple name of the generated class.
   *
   * <p>If empty, the schema class name with a trailing {@code ConfigSchema} or {@code Schema}
   * replaced by {@code Wrapper}:
   * <ul>
   *   <li>AbilitiesConfigSchema → AbilitiesWrapper</li>
   *   <li>CTFSchema → CTFWrapper</li>
   * </ul>
   */
  String value() default "";

  /**
   * The package of the generated class.
   *
   * <p>If empty, a {@code wrappers} package inside the schema's package.
   */
  String packageName() default "";

  /**
   * Whether to generate a {@code reload()} method. Accessors never cache, so the method does
   * nothing; it exists so code written for earlier wrappers still compiles.
   */
  boolean generateReload() default true;

  /**
   * Whether the generated class is {@code final}.
   */
  boolean makeFinal() default true;
}
