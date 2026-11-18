package me.mortaldev.abstractyaml;

import java.util.List;
import java.util.function.Predicate;

/**
 * Ready-made validators for legacy {@link ConfigValue}s of type {@code List}.
 *
 * <pre>{@code
 * new ConfigValue<>("worlds", List.class, List.of("world"))
 *     .setValidator(ListValidators.allStringsNotEmpty());
 * }</pre>
 *
 * <p>Every validator starts with a {@code null} check (message {@code "Value cannot be null"}).
 * Rules stop at the first failure, so a list with a wrong element type reports the type rule,
 * not the range rule after it. The validators use the raw {@code List} type, matching
 * {@code List.class}.
 *
 * @deprecated Part of the legacy config system. Use the list validators on
 *     {@code me.mortaldev.abstractyaml.config2.Validator}.
 */
@Deprecated
public class ListValidators {

  /** Not needed: every method is static. Kept public so existing code still compiles. */
  public ListValidators() {}

  /**
   * Requires at least {@code minSize} elements. Message:
   * {@code "List must have at least <minSize> elements"}.
   *
   * @param minSize the smallest allowed size
   * @return a new validator
   */
  public static ConfigValidator<List> minSize(int minSize) {
    return ConfigValidator.<List>notNull()
        .addRule(list -> list.size() >= minSize, "List must have at least " + minSize + " elements");
  }

  /**
   * Requires at most {@code maxSize} elements. Message:
   * {@code "List cannot exceed <maxSize> elements"}.
   *
   * @param maxSize the largest allowed size
   * @return a new validator
   */
  public static ConfigValidator<List> maxSize(int maxSize) {
    return ConfigValidator.<List>notNull()
        .addRule(list -> list.size() <= maxSize, "List cannot exceed " + maxSize + " elements");
  }

  /**
   * Requires a size between {@code min} and {@code max}, both included. Message:
   * {@code "List size must be between <min> and <max>"}.
   *
   * @param min the smallest allowed size
   * @param max the largest allowed size
   * @return a new validator
   */
  public static ConfigValidator<List> sizeRange(int min, int max) {
    return ConfigValidator.<List>notNull()
        .addRule(
            list -> list.size() >= min && list.size() <= max,
            "List size must be between " + min + " and " + max);
  }

  /**
   * Requires every element to be an instance of {@code type}; {@code null} elements fail.
   * Message: {@code "All elements must be of type <SimpleName>"}.
   *
   * @param type the required element class, such as {@code String.class}
   * @return a new validator
   */
  public static ConfigValidator<List> allInstanceOf(Class<?> type) {
    return ConfigValidator.<List>notNull()
        .addRule(
            list -> list.stream().allMatch(type::isInstance),
            "All elements must be of type " + type.getSimpleName());
  }

  /**
   * Requires every element to be text with at least one non-whitespace character. Messages:
   * {@code "All elements must be strings"}, then {@code "All strings must be non-empty"}.
   *
   * @return a new validator
   */
  public static ConfigValidator<List> allStringsNotEmpty() {
    return ConfigValidator.<List>notNull()
        .addRule(list -> list.stream().allMatch(obj -> obj instanceof String), "All elements must be strings")
        .addRule(
            list -> {
              for (Object obj : list) {
                String str = (String) obj;
                if (str == null || str.trim().isEmpty()) {
                  return false;
                }
              }
              return true;
            },
            "All strings must be non-empty");
  }

  /**
   * Requires every element to be an {@code Integer} between {@code min} and {@code max}, both
   * included. Messages: {@code "All elements must be integers"}, then
   * {@code "All integers must be between <min> and <max>"}.
   *
   * @param min the smallest allowed value
   * @param max the largest allowed value
   * @return a new validator
   */
  public static ConfigValidator<List> allIntegersInRange(int min, int max) {
    return ConfigValidator.<List>notNull()
        .addRule(list -> list.stream().allMatch(obj -> obj instanceof Integer), "All elements must be integers")
        .addRule(
            list -> {
              for (Object obj : list) {
                Integer i = (Integer) obj;
                if (i < min || i > max) {
                  return false;
                }
              }
              return true;
            },
            "All integers must be between " + min + " and " + max);
  }

  /**
   * Requires every element to be a number (any {@code Number}) between {@code min} and
   * {@code max}, both included. Messages: {@code "All elements must be numbers"}, then
   * {@code "All numbers must be between <min> and <max>"}.
   *
   * @param min the smallest allowed value
   * @param max the largest allowed value
   * @return a new validator
   */
  public static ConfigValidator<List> allDoublesInRange(double min, double max) {
    return ConfigValidator.<List>notNull()
        .addRule(
            list -> list.stream().allMatch(obj -> obj instanceof Number),
            "All elements must be numbers")
        .addRule(
            list -> {
              for (Object obj : list) {
                Number num = (Number) obj;
                double d = num.doubleValue();
                if (d < min || d > max) {
                  return false;
                }
              }
              return true;
            },
            "All numbers must be between " + min + " and " + max);
  }

  /**
   * Requires every element to be an instance of {@code type} and to pass {@code predicate}.
   * Messages: {@code "All elements must be of type <SimpleName>"}, then {@code errorMessage}.
   *
   * <pre>{@code
   * ListValidators.allMatch(String.class, s -> s.startsWith("minecraft:"), "Use namespaced IDs")
   * }</pre>
   *
   * @param type the required element class
   * @param predicate returns {@code true} for valid elements
   * @param errorMessage the message when an element fails {@code predicate}
   * @param <T> the element type
   * @return a new validator
   */
  public static <T> ConfigValidator<List> allMatch(Class<T> type, Predicate<T> predicate, String errorMessage) {
    return ConfigValidator.<List>notNull()
        .addRule(
            list -> list.stream().allMatch(type::isInstance),
            "All elements must be of type " + type.getSimpleName())
        .addRule(
            list -> {
              for (Object obj : list) {
                T item = type.cast(obj);
                if (!predicate.test(item)) {
                  return false;
                }
              }
              return true;
            },
            errorMessage);
  }

  /**
   * Requires no repeated elements, compared with {@code equals} (case-sensitive for text).
   * Message: {@code "List cannot contain duplicate elements"}.
   *
   * @return a new validator
   */
  public static ConfigValidator<List> noDuplicates() {
    return ConfigValidator.<List>notNull()
        .addRule(
            list -> list.size() == list.stream().distinct().count(),
            "List cannot contain duplicate elements");
  }

  /**
   * Requires every element to be a {@code Boolean}; {@code null} elements fail. Message:
   * {@code "All elements must be booleans"}.
   *
   * @return a new validator
   */
  public static ConfigValidator<List> allBooleansValid() {
    return ConfigValidator.<List>notNull()
        .addRule(
            list -> list.stream().allMatch(obj -> obj instanceof Boolean),
            "All elements must be booleans");
  }

  /**
   * Requires every element to be text that matches {@code regex} in full. Messages:
   * {@code "All elements must be strings"}, then {@code "All strings must match pattern: <regex>"}.
   *
   * @param regex a {@link java.util.regex.Pattern} expression, compiled on each check
   * @return a new validator
   */
  public static ConfigValidator<List> allStringsMatch(String regex) {
    return ConfigValidator.<List>notNull()
        .addRule(list -> list.stream().allMatch(obj -> obj instanceof String), "All elements must be strings")
        .addRule(
            list -> {
              for (Object obj : list) {
                String str = (String) obj;
                if (!str.matches(regex)) {
                  return false;
                }
              }
              return true;
            },
            "All strings must match pattern: " + regex);
  }

  /**
   * Requires a list of text whose size is between {@code minSize} and {@code maxSize}. Same as
   * {@link #sizeRange(int, int)} followed by a check that every element is text (message
   * {@code "All elements must be strings"}). Empty text is allowed.
   *
   * @param minSize the smallest allowed size
   * @param maxSize the largest allowed size
   * @return a new validator
   */
  public static ConfigValidator<List> stringList(int minSize, int maxSize) {
    return sizeRange(minSize, maxSize)
        .addRule(
            list -> list.stream().allMatch(obj -> obj instanceof String),
            "All elements must be strings");
  }

  /**
   * Requires a list of {@code Integer}s whose size is between {@code minSize} and
   * {@code maxSize} and whose elements are between {@code minValue} and {@code maxValue}, all
   * bounds included.
   *
   * @param minSize the smallest allowed size
   * @param maxSize the largest allowed size
   * @param minValue the smallest allowed element
   * @param maxValue the largest allowed element
   * @return a new validator
   */
  public static ConfigValidator<List> integerList(int minSize, int maxSize, int minValue, int maxValue) {
    return sizeRange(minSize, maxSize)
        .addRule(list -> list.stream().allMatch(obj -> obj instanceof Integer), "All elements must be integers")
        .addRule(
            list -> {
              for (Object obj : list) {
                Integer i = (Integer) obj;
                if (i < minValue || i > maxValue) {
                  return false;
                }
              }
              return true;
            },
            "All integers must be between " + minValue + " and " + maxValue);
  }
}
