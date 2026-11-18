package me.mortaldev.abstractyaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A list of rules a legacy {@link ConfigValue} must follow. Rules run in the order they were
 * added and stop at the first one that fails, so only that rule's message is reported.
 *
 * <pre>{@code
 * new ConfigValue<>("prefix", String.class, "[Server]")
 *     .setValidator(ConfigValidator.notEmpty()
 *         .addRule(s -> s.length() <= 16, "Prefix must be 16 characters or fewer"));
 * }</pre>
 *
 * <p>The static methods each return a new validator with one rule; {@link #addRule} adds more.
 * Number rules ({@link #range}, {@link #min}, {@link #max}) work with any {@code Number} that is
 * {@code Comparable}; the bounds must have the value's type ({@code range(1, 10)} for
 * {@code Integer}, {@code range(1.0, 10.0)} for {@code Double}). Most rules throw
 * {@link NullPointerException} for a {@code null} value; put {@link #notNull()} first if the
 * value can be {@code null}.
 *
 * @param <T> the type of value to validate
 * @deprecated Part of the legacy config system. Use {@code me.mortaldev.abstractyaml.config2.Validator}.
 */
@Deprecated
public class ConfigValidator<T> {
  private final List<ValidationRule<T>> rules = new ArrayList<>();

  /** Creates a validator with no rules; every value passes until rules are added. */
  public ConfigValidator() {}

  /**
   * Adds a rule after the existing ones. The validator is changed and returned.
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
   * @throws ValidationException if validation fails; the message is
   *     {@code "Validation failed: <rule message>"}
   */
  public void validateOrThrow(T value) {
    ValidationResult result = validate(value);
    if (!result.isValid()) {
      throw new ValidationException("Validation failed: " + String.join(", ", result.errors()));
    }
  }

  // ===== Common Validators =====

  /**
   * Requires a number between {@code min} and {@code max}, both included. Message:
   * {@code "Value must be between <min> and <max>"}.
   *
   * @param min the smallest allowed value
   * @param max the largest allowed value
   * @param <N> the number type, such as {@code Integer} or {@code Double}
   * @return a new validator with this rule
   */
  public static <N extends Number & Comparable<N>> ConfigValidator<N> range(N min, N max) {
    return new ConfigValidator<N>()
        .addRule(
            value -> value.compareTo(min) >= 0 && value.compareTo(max) <= 0,
            "Value must be between " + min + " and " + max);
  }

  /**
   * Requires a number of at least {@code min}. Message: {@code "Value must be at least <min>"}.
   *
   * @param min the smallest allowed value
   * @param <N> the number type, such as {@code Integer} or {@code Double}
   * @return a new validator with this rule
   */
  public static <N extends Number & Comparable<N>> ConfigValidator<N> min(N min) {
    return new ConfigValidator<N>()
        .addRule(value -> value.compareTo(min) >= 0, "Value must be at least " + min);
  }

  /**
   * Requires a number of at most {@code max}. Message: {@code "Value must be at most <max>"}.
   *
   * @param max the largest allowed value
   * @param <N> the number type, such as {@code Integer} or {@code Double}
   * @return a new validator with this rule
   */
  public static <N extends Number & Comparable<N>> ConfigValidator<N> max(N max) {
    return new ConfigValidator<N>()
        .addRule(value -> value.compareTo(max) <= 0, "Value must be at most " + max);
  }

  /**
   * Requires text with at least one non-whitespace character; {@code null} fails too. Message:
   * {@code "Value cannot be empty"}.
   *
   * @return a new validator with this rule
   */
  public static ConfigValidator<String> notEmpty() {
    return new ConfigValidator<String>()
        .addRule(value -> value != null && !value.trim().isEmpty(), "Value cannot be empty");
  }

  /**
   * Requires a text length between {@code min} and {@code max}, both included. Message:
   * {@code "Length must be between <min> and <max>"}.
   *
   * @param min the shortest allowed length
   * @param max the longest allowed length
   * @return a new validator with this rule
   */
  public static ConfigValidator<String> length(int min, int max) {
    return new ConfigValidator<String>()
        .addRule(
            value -> value.length() >= min && value.length() <= max,
            "Length must be between " + min + " and " + max);
  }

  /**
   * Requires the text to match a regular expression in full ({@code matches("[a-z]+")} rejects
   * {@code "abc1"}). Message: {@code "Value must match pattern: <regex>"}.
   *
   * @param regex a {@link Pattern} expression
   * @return a new validator with this rule
   * @throws java.util.regex.PatternSyntaxException if {@code regex} is invalid
   */
  public static ConfigValidator<String> matches(String regex) {
    Pattern pattern = Pattern.compile(regex);
    return new ConfigValidator<String>()
        .addRule(value -> pattern.matcher(value).matches(), "Value must match pattern: " + regex);
  }

  /**
   * Requires the value to equal one of the given values, compared with {@code equals}
   * (case-sensitive for text). Message: {@code "Value must be one of: <a>, <b>, ..."}.
   *
   * @param allowedValues the accepted values
   * @param <T> the type of value
   * @return a new validator with this rule
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
   * Requires a value that is not {@code null}. Message: {@code "Value cannot be null"}. Add
   * other rules after it with {@link #addRule}.
   *
   * @param <T> the type of value
   * @return a new validator with this rule
   */
  public static <T> ConfigValidator<T> notNull() {
    return new ConfigValidator<T>().addRule(value -> value != null, "Value cannot be null");
  }

  /**
   * The outcome of {@link ConfigValidator#validate(Object)}.
   *
   * @param isValid whether the value passed every rule
   * @param errors the message of the rule that failed; empty when valid
   */
  public record ValidationResult(boolean isValid, List<String> errors) {

    /**
     * Returns the error messages joined with {@code ", "}.
     *
     * @return the messages; an empty string when valid
     */
    public String getErrorMessage() {
      return String.join(", ", errors);
    }
  }

  /**
   * Represents a single validation rule.
   */
  private record ValidationRule<T>(Predicate<T> predicate, String errorMessage) {}

  /**
   * Thrown by {@link ConfigValidator#validateOrThrow(Object)} when a value fails.
   */
  public static class ValidationException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message what failed
     */
    public ValidationException(String message) {
      super(message);
    }
  }
}
