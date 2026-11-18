package me.mortaldev.abstractyaml.config2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A rule a config value must follow, such as "between 1 and 300" or "not empty".
 *
 * <p>Pass a validator when declaring a value:
 *
 * <pre>{@code
 * section("fireball")
 *     .intValue("cooldown", 10, Validator.range(1, 300), "Seconds between casts")
 *     .stringValue("name", "Fireball", Validator.notEmpty().and(Validator.length(1, 32)), null);
 * }</pre>
 *
 * <p>Validators run when a config loads and in {@link Config#with(String, Object)}. If a loaded
 * value fails, loading fails with {@link ConfigLoader.ConfigLoadException}, whose message
 * lists every failing value as {@code "path: message"}. The value's default must also pass, or
 * the config cannot load at all.
 *
 * <p>Validators only see values that already have the expected type: data the loader cannot
 * convert (text in an int value, for example) is replaced by the default before validation. A
 * value loaded from a file is never {@code null}, so the validators below only handle
 * {@code null} where they say so.
 *
 * <p>Write your own with a lambda or {@link #of(Predicate, String)}:
 *
 * <pre>{@code
 * Validator<Integer> even = Validator.of(i -> i % 2 == 0, "Value must be even");
 * Validator<String> lowercase = value -> value.equals(value.toLowerCase())
 *     ? ValidationResult.valid()
 *     : ValidationResult.invalid("Value must be lowercase");
 * }</pre>
 *
 * <p>Number validators have separate {@code int} and {@code double} overloads. For a
 * {@code double} value, use double literals: {@code Validator.min(0.0)}. {@code Validator.min(0)}
 * is the {@code Integer} version and does not compile for a {@code double} value.
 *
 * <p>Validators have no state and can be shared between values and threads.
 *
 * @param <T> the type of value to validate
 */
@FunctionalInterface
public interface Validator<T> {

  /**
   * Checks a value.
   *
   * @param value the value to validate
   * @return {@link ValidationResult#valid()}, or {@link ValidationResult#invalid(String)} with
   *     a message saying what is wrong
   */
  ValidationResult validate(T value);

  /**
   * Returns a validator that accepts every value. This is what a value without a validator
   * uses.
   *
   * @param <T> the type of value
   * @return a validator that always passes
   */
  static <T> Validator<T> none() {
    return value -> ValidationResult.valid();
  }

  /**
   * Returns a validator that requires this one and {@code other} to pass. They run in order:
   * if this one fails, its result is returned and {@code other} does not run, so only the first
   * failing message is reported.
   *
   * <pre>{@code
   * Validator.notEmpty().and(Validator.matches("[a-z_]+"))
   * }</pre>
   *
   * @param other the validator to run after this one
   * @return the combined validator
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
   * Returns a validator that passes when {@code predicate} returns {@code true} for the value.
   *
   * <pre>{@code
   * Validator.of(name -> !name.contains(" "), "Value cannot contain spaces")
   * }</pre>
   *
   * @param predicate returns {@code true} for valid values
   * @param errorMessage the message reported when the predicate returns {@code false}
   * @param <T> the type of value
   * @return the validator
   */
  static <T> Validator<T> of(Predicate<T> predicate, String errorMessage) {
    return value -> predicate.test(value)
        ? ValidationResult.valid()
        : ValidationResult.invalid(errorMessage);
  }

  // ===== Common Validators =====

  /**
   * Requires a value that is not {@code null}. Message: {@code "Value cannot be null"}.
   *
   * @param <T> the type of value
   * @return the validator
   */
  static <T> Validator<T> notNull() {
    return of(value -> value != null, "Value cannot be null");
  }

  /**
   * Requires text with at least one non-whitespace character; {@code null}, {@code ""} and
   * {@code "   "} fail. Message: {@code "Value cannot be empty"}.
   *
   * @return the validator
   */
  static Validator<String> notEmpty() {
    return of(s -> s != null && !s.trim().isEmpty(), "Value cannot be empty");
  }

  /**
   * Requires the text to match a regular expression in full, as in
   * {@link java.util.regex.Matcher#matches()}: {@code matches("[a-z]+")} accepts
   * {@code "abc"} but not {@code "abc1"}. Message: {@code "Value must match pattern: <regex>"}.
   *
   * @param regex a {@link Pattern} expression
   * @return the validator
   * @throws java.util.regex.PatternSyntaxException if {@code regex} is not a valid expression;
   *     thrown when the validator is created, so the schema fails to construct
   */
  static Validator<String> matches(String regex) {
    Pattern pattern = Pattern.compile(regex);
    return of(s -> pattern.matcher(s).matches(), "Value must match pattern: " + regex);
  }

  /**
   * Requires the text length ({@link String#length()}) to be between {@code min} and
   * {@code max}, both included. Message: {@code "Length must be between <min> and <max>"}.
   *
   * @param min the shortest allowed length
   * @param max the longest allowed length
   * @return the validator
   */
  static Validator<String> length(int min, int max) {
    return of(
        s -> s.length() >= min && s.length() <= max,
        "Length must be between " + min + " and " + max);
  }

  /**
   * Requires a whole number between {@code min} and {@code max}, both included. Message:
   * {@code "Value must be between <min> and <max>"}.
   *
   * @param min the smallest allowed value
   * @param max the largest allowed value
   * @return the validator
   */
  static Validator<Integer> range(int min, int max) {
    return of(
        i -> i >= min && i <= max,
        "Value must be between " + min + " and " + max);
  }

  /**
   * Requires a whole number of at least {@code min} ({@code min} itself passes). Message:
   * {@code "Value must be at least <min>"}.
   *
   * @param min the smallest allowed value
   * @return the validator
   */
  static Validator<Integer> min(int min) {
    return of(i -> i >= min, "Value must be at least " + min);
  }

  /**
   * Requires a whole number of at most {@code max} ({@code max} itself passes). Message:
   * {@code "Value must be at most <max>"}.
   *
   * @param max the largest allowed value
   * @return the validator
   */
  static Validator<Integer> max(int max) {
    return of(i -> i <= max, "Value must be at most " + max);
  }

  /**
   * Requires a decimal number between {@code min} and {@code max}, both included.
   * {@code NaN} fails. Message: {@code "Value must be between <min> and <max>"}.
   *
   * @param min the smallest allowed value
   * @param max the largest allowed value
   * @return the validator
   */
  static Validator<Double> range(double min, double max) {
    return of(
        d -> d >= min && d <= max,
        "Value must be between " + min + " and " + max);
  }

  /**
   * Requires a decimal number of at least {@code min} ({@code min} itself passes). {@code NaN}
   * fails. Message: {@code "Value must be at least <min>"}.
   *
   * @param min the smallest allowed value
   * @return the validator
   */
  static Validator<Double> min(double min) {
    return of(d -> d >= min, "Value must be at least " + min);
  }

  /**
   * Requires a decimal number of at most {@code max} ({@code max} itself passes). {@code NaN}
   * fails. Message: {@code "Value must be at most <max>"}.
   *
   * @param max the largest allowed value
   * @return the validator
   */
  static Validator<Double> max(double max) {
    return of(d -> d <= max, "Value must be at most " + max);
  }

  /**
   * Requires the value to equal one of the given values, compared with {@code equals}. Text
   * comparison is case-sensitive: {@code oneOf("easy", "hard")} rejects {@code "Easy"}. Message:
   * {@code "Value must be one of: [<values>]"}.
   *
   * @param allowedValues the accepted values
   * @param <T> the type of value
   * @return the validator
   */
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

  /**
   * Requires a list with at least {@code minSize} elements. Message:
   * {@code "List must have at least <minSize> elements"}.
   *
   * @param minSize the smallest allowed size
   * @param <T> the element type
   * @return the validator
   */
  static <T> Validator<List<T>> minSize(int minSize) {
    return of(list -> list.size() >= minSize, "List must have at least " + minSize + " elements");
  }

  /**
   * Requires a list with at most {@code maxSize} elements. Message:
   * {@code "List cannot exceed <maxSize> elements"}.
   *
   * @param maxSize the largest allowed size
   * @param <T> the element type
   * @return the validator
   */
  static <T> Validator<List<T>> maxSize(int maxSize) {
    return of(list -> list.size() <= maxSize, "List cannot exceed " + maxSize + " elements");
  }

  /**
   * Requires a list size between {@code min} and {@code max}, both included. Message:
   * {@code "List size must be between <min> and <max>"}.
   *
   * @param min the smallest allowed size
   * @param max the largest allowed size
   * @param <T> the element type
   * @return the validator
   */
  static <T> Validator<List<T>> sizeRange(int min, int max) {
    return of(
        list -> list.size() >= min && list.size() <= max,
        "List size must be between " + min + " and " + max);
  }

  /**
   * Requires every text in a list to have at least one non-whitespace character. An empty list
   * passes. Message: {@code "All strings must be non-empty"}.
   *
   * @return the validator
   */
  static Validator<List<String>> allStringsNotEmpty() {
    return of(
        list -> list.stream().allMatch(s -> s != null && !s.trim().isEmpty()),
        "All strings must be non-empty");
  }

  /**
   * Requires every text in a list to match a regular expression in full, as in
   * {@link #matches(String)}. An empty list passes. Message:
   * {@code "All strings must match pattern: <regex>"}.
   *
   * @param regex a {@link Pattern} expression
   * @return the validator
   * @throws java.util.regex.PatternSyntaxException if {@code regex} is not a valid expression;
   *     thrown when the validator is created
   */
  static Validator<List<String>> allStringsMatch(String regex) {
    Pattern pattern = Pattern.compile(regex);
    return of(
        list -> list.stream().allMatch(s -> pattern.matcher(s).matches()),
        "All strings must match pattern: " + regex);
  }

  /**
   * Requires every whole number in a list to be between {@code min} and {@code max}, both
   * included. An empty list passes. Message:
   * {@code "All integers must be between <min> and <max>"}.
   *
   * @param min the smallest allowed value
   * @param max the largest allowed value
   * @return the validator
   */
  static Validator<List<Integer>> allIntegersInRange(int min, int max) {
    return of(
        list -> list.stream().allMatch(i -> i >= min && i <= max),
        "All integers must be between " + min + " and " + max);
  }

  /**
   * Requires every decimal number in a list to be between {@code min} and {@code max}, both
   * included. An empty list passes. Message:
   * {@code "All numbers must be between <min> and <max>"}.
   *
   * @param min the smallest allowed value
   * @param max the largest allowed value
   * @return the validator
   */
  static Validator<List<Double>> allDoublesInRange(double min, double max) {
    return of(
        list -> list.stream().allMatch(d -> d >= min && d <= max),
        "All numbers must be between " + min + " and " + max);
  }

  /**
   * Requires a list without repeated elements, compared with {@code equals}. Text comparison is
   * case-sensitive, so {@code "a"} and {@code "A"} are different. Message:
   * {@code "List cannot contain duplicates"}.
   *
   * @param <T> the element type
   * @return the validator
   */
  static <T> Validator<List<T>> noDuplicates() {
    return of(
        list -> list.size() == list.stream().distinct().count(),
        "List cannot contain duplicates");
  }

  /**
   * Requires every element of a list to pass {@code predicate}. An empty list passes.
   *
   * <pre>{@code
   * Validator.<String>allMatch(item -> Material.matchMaterial(item) != null,
   *     "Every item must be a valid material")
   * }</pre>
   *
   * @param predicate returns {@code true} for valid elements
   * @param errorMessage the message reported when any element fails
   * @param <T> the element type
   * @return the validator
   */
  static <T> Validator<List<T>> allMatch(Predicate<T> predicate, String errorMessage) {
    return of(list -> list.stream().allMatch(predicate), errorMessage);
  }
}
