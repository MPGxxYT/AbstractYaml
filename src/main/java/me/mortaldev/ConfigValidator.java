package me.mortaldev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Provides validation functionality for configuration values.
 *
 * @param <T> the type of value to validate
 * @deprecated Part of the legacy config system. Use {@code me.mortaldev.config2.Validator}.
 */
@Deprecated
public class ConfigValidator<T> {
  private final List<ValidationRule<T>> rules = new ArrayList<>();

  /**
   * Adds a validation rule with a custom predicate.
   *
   * @param predicate the validation predicate
   * @param errorMessage the error message if validation fails
   * @return this validator for method chaining
   */
  public ConfigValidator<T> addRule(Predicate<T> predicate, String errorMessage) {
    rules.add(new ValidationRule<>(predicate, errorMessage));
    return this;
  }

  /**
   * Validates the given value against the rules in the order they were added, stopping at the
   * first rule that fails. Later rules can therefore rely on earlier ones (for example, a size
   * rule after {@link #notNull()}, or a cast after an element-type rule).
   *
   * @param value the value to validate
   * @return a ValidationResult containing the first error, if any
   */
  public ValidationResult validate(T value) {
    for (ValidationRule<T> rule : rules) {
      if (!rule.predicate().test(value)) {
        return new ValidationResult(false, List.of(rule.errorMessage()));
      }
    }
    return new ValidationResult(true, List.of());
  }

  /**
   * Validates the value and throws an exception if invalid.
   *
   * @param value the value to validate
   * @throws ValidationException if validation fails
   */
  public void validateOrThrow(T value) {
    ValidationResult result = validate(value);
    if (!result.isValid()) {
      throw new ValidationException("Validation failed: " + String.join(", ", result.errors()));
    }
  }

  // ===== Common Validators =====

  /**
   * Creates a validator for numeric ranges.
   */
  public static <N extends Number & Comparable<N>> ConfigValidator<N> range(N min, N max) {
    return new ConfigValidator<N>()
        .addRule(
            value -> value.compareTo(min) >= 0 && value.compareTo(max) <= 0,
            "Value must be between " + min + " and " + max);
  }

  /**
   * Creates a validator for minimum values.
   */
  public static <N extends Number & Comparable<N>> ConfigValidator<N> min(N min) {
    return new ConfigValidator<N>()
        .addRule(value -> value.compareTo(min) >= 0, "Value must be at least " + min);
  }

  /**
   * Creates a validator for maximum values.
   */
  public static <N extends Number & Comparable<N>> ConfigValidator<N> max(N max) {
    return new ConfigValidator<N>()
        .addRule(value -> value.compareTo(max) <= 0, "Value must be at most " + max);
  }

  /**
   * Creates a validator for non-empty strings.
   */
  public static ConfigValidator<String> notEmpty() {
    return new ConfigValidator<String>()
        .addRule(value -> value != null && !value.trim().isEmpty(), "Value cannot be empty");
  }

  /**
   * Creates a validator for string length.
   */
  public static ConfigValidator<String> length(int min, int max) {
    return new ConfigValidator<String>()
        .addRule(
            value -> value.length() >= min && value.length() <= max,
            "Length must be between " + min + " and " + max);
  }

  /**
   * Creates a validator for regex pattern matching.
   */
  public static ConfigValidator<String> matches(String regex) {
    Pattern pattern = Pattern.compile(regex);
    return new ConfigValidator<String>()
        .addRule(value -> pattern.matcher(value).matches(), "Value must match pattern: " + regex);
  }

  /**
   * Creates a validator that checks if a value is in a set of allowed values.
   */
  @SafeVarargs
  public static <T> ConfigValidator<T> oneOf(T... allowedValues) {
    return new ConfigValidator<T>()
        .addRule(
            value -> {
              for (T allowed : allowedValues) {
                if (allowed.equals(value)) {
                  return true;
                }
              }
              return false;
            },
            "Value must be one of: "
                + Arrays.stream(allowedValues).map(String::valueOf).collect(Collectors.joining(", ")));
  }

  /**
   * Creates a validator that ensures the value is not null.
   */
  public static <T> ConfigValidator<T> notNull() {
    return new ConfigValidator<T>().addRule(value -> value != null, "Value cannot be null");
  }

  /**
   * Represents the result of a validation operation.
   */
  public record ValidationResult(boolean isValid, List<String> errors) {
    public String getErrorMessage() {
      return String.join(", ", errors);
    }
  }

  /**
   * Represents a single validation rule.
   */
  private record ValidationRule<T>(Predicate<T> predicate, String errorMessage) {}

  /**
   * Exception thrown when validation fails.
   */
  public static class ValidationException extends RuntimeException {
    public ValidationException(String message) {
      super(message);
    }
  }
}
