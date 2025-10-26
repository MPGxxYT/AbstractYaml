package me.mortaldev;

import java.util.List;
import java.util.function.Predicate;

/**
 * Helper class providing common validators for List-type ConfigValues.
 */
public class ListValidators {

  /**
   * Validates that a list is not empty and has at least the specified number of elements.
   */
  public static ConfigValidator<List> minSize(int minSize) {
    return ConfigValidator.<List>notNull()
        .addRule(list -> list.size() >= minSize, "List must have at least " + minSize + " elements");
  }

  /**
   * Validates that a list does not exceed the specified number of elements.
   */
  public static ConfigValidator<List> maxSize(int maxSize) {
    return ConfigValidator.<List>notNull()
        .addRule(list -> list.size() <= maxSize, "List cannot exceed " + maxSize + " elements");
  }

  /**
   * Validates that a list size is within the specified range.
   */
  public static ConfigValidator<List> sizeRange(int min, int max) {
    return ConfigValidator.<List>notNull()
        .addRule(
            list -> list.size() >= min && list.size() <= max,
            "List size must be between " + min + " and " + max);
  }

  /**
   * Validates that all elements in the list are of the specified type.
   */
  public static ConfigValidator<List> allInstanceOf(Class<?> type) {
    return ConfigValidator.<List>notNull()
        .addRule(
            list -> list.stream().allMatch(type::isInstance),
            "All elements must be of type " + type.getSimpleName());
  }

  /**
   * Validates that all String elements in the list are not empty.
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
   * Validates that all Integer elements in the list are within the specified range.
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
   * Validates that all Double/Number elements in the list are within the specified range.
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
   * Validates that all elements in the list match the given predicate.
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
   * Validates that the list contains no duplicate elements.
   */
  public static ConfigValidator<List> noDuplicates() {
    return ConfigValidator.<List>notNull()
        .addRule(
            list -> list.size() == list.stream().distinct().count(),
            "List cannot contain duplicate elements");
  }

  /**
   * Validates that all Boolean elements exist (no nulls).
   */
  public static ConfigValidator<List> allBooleansValid() {
    return ConfigValidator.<List>notNull()
        .addRule(
            list -> list.stream().allMatch(obj -> obj instanceof Boolean),
            "All elements must be booleans");
  }

  /**
   * Validates that String list elements match a regex pattern.
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
   * Combines multiple validators for common List<String> use cases.
   */
  public static ConfigValidator<List> stringList(int minSize, int maxSize) {
    return sizeRange(minSize, maxSize)
        .addRule(
            list -> list.stream().allMatch(obj -> obj instanceof String),
            "All elements must be strings");
  }

  /**
   * Combines multiple validators for common List<Integer> use cases.
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
