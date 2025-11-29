package me.mortaldev.config2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Validates configuration values.
 * Validators are immutable and can be chained.
 *
 * @param <T> the type of value to validate
 */
@FunctionalInterface
public interface Validator<T> {

  /**
   * Validates the given value.
   *
   * @param value the value to validate
   * @return the validation result
   */
  ValidationResult validate(T value);

  /**
   * A validator that always passes.
   */
  static <T> Validator<T> none() {
    return value -> ValidationResult.valid();
  }

  /**
   * Chains this validator with another.
   * Both validators must pass for the result to be valid.
   */
  default Validator<T> and(Validator<T> other) {
    return value -> {
      ValidationResult first = this.validate(value);
      if (!first.isValid()) {
        return first;
      }
      return other.validate(value);
    };
  }

  /**
   * Creates a validator from a predicate.
   */
  static <T> Validator<T> of(Predicate<T> predicate, String errorMessage) {
    return value -> predicate.test(value)
        ? ValidationResult.valid()
        : ValidationResult.invalid(errorMessage);
  }

  // ===== Common Validators =====

  /** Validates that a value is not null. */
  static <T> Validator<T> notNull() {
    return of(value -> value != null, "Value cannot be null");
  }

  /** Validates that a string is not empty. */
  static Validator<String> notEmpty() {
    return of(s -> s != null && !s.trim().isEmpty(), "Value cannot be empty");
  }

  /** Validates that a string matches a regex pattern. */
  static Validator<String> matches(String regex) {
    Pattern pattern = Pattern.compile(regex);
    return of(s -> pattern.matcher(s).matches(), "Value must match pattern: " + regex);
  }

  /** Validates that a string length is within range. */
  static Validator<String> length(int min, int max) {
    return of(
        s -> s.length() >= min && s.length() <= max,
        "Length must be between " + min + " and " + max);
  }

  /** Validates that an integer is within a range (inclusive). */
  static Validator<Integer> range(int min, int max) {
    return of(
        i -> i >= min && i <= max,
        "Value must be between " + min + " and " + max);
  }

  /** Validates that an integer is at least a minimum value. */
  static Validator<Integer> min(int min) {
    return of(i -> i >= min, "Value must be at least " + min);
  }

  /** Validates that an integer is at most a maximum value. */
  static Validator<Integer> max(int max) {
    return of(i -> i <= max, "Value must be at most " + max);
  }

  /** Validates that a double is within a range (inclusive). */
  static Validator<Double> range(double min, double max) {
    return of(
        d -> d >= min && d <= max,
        "Value must be between " + min + " and " + max);
  }

  /** Validates that a double is at least a minimum value. */
  static Validator<Double> min(double min) {
    return of(d -> d >= min, "Value must be at least " + min);
  }

  /** Validates that a double is at most a maximum value. */
  static Validator<Double> max(double max) {
    return of(d -> d <= max, "Value must be at most " + max);
  }

  /** Validates that a value is one of the allowed values. */
  @SafeVarargs
  static <T> Validator<T> oneOf(T... allowedValues) {
    return of(
        value -> {
          for (T allowed : allowedValues) {
            if (allowed.equals(value)) {
              return true;
            }
          }
          return false;
        },
        "Value must be one of: " + java.util.Arrays.toString(allowedValues));
  }

  // ===== List Validators =====

  /** Validates that a list has at least a minimum size. */
  static <T> Validator<List<T>> minSize(int minSize) {
    return of(list -> list.size() >= minSize, "List must have at least " + minSize + " elements");
  }

  /** Validates that a list does not exceed a maximum size. */
  static <T> Validator<List<T>> maxSize(int maxSize) {
    return of(list -> list.size() <= maxSize, "List cannot exceed " + maxSize + " elements");
  }

  /** Validates that a list size is within a range. */
  static <T> Validator<List<T>> sizeRange(int min, int max) {
    return of(
        list -> list.size() >= min && list.size() <= max,
        "List size must be between " + min + " and " + max);
  }

  /** Validates that all strings in a list are not empty. */
  static Validator<List<String>> allStringsNotEmpty() {
    return of(
        list -> list.stream().allMatch(s -> s != null && !s.trim().isEmpty()),
        "All strings must be non-empty");
  }

  /** Validates that all strings in a list match a pattern. */
  static Validator<List<String>> allStringsMatch(String regex) {
    Pattern pattern = Pattern.compile(regex);
    return of(
        list -> list.stream().allMatch(s -> pattern.matcher(s).matches()),
        "All strings must match pattern: " + regex);
  }

  /** Validates that all integers in a list are within a range. */
  static Validator<List<Integer>> allIntegersInRange(int min, int max) {
    return of(
        list -> list.stream().allMatch(i -> i >= min && i <= max),
        "All integers must be between " + min + " and " + max);
  }

  /** Validates that all doubles in a list are within a range. */
  static Validator<List<Double>> allDoublesInRange(double min, double max) {
    return of(
        list -> list.stream().allMatch(d -> d >= min && d <= max),
        "All numbers must be between " + min + " and " + max);
  }

  /** Validates that a list contains no duplicates. */
  static <T> Validator<List<T>> noDuplicates() {
    return of(
        list -> list.size() == list.stream().distinct().count(),
        "List cannot contain duplicates");
  }

  /** Validates that all elements in a list match a predicate. */
  static <T> Validator<List<T>> allMatch(Predicate<T> predicate, String errorMessage) {
    return of(list -> list.stream().allMatch(predicate), errorMessage);
  }
}
