package me.mortaldev.abstractyaml.config2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The outcome of checking a value with a {@link Validator}: valid, or invalid with one or more
 * error messages.
 *
 * <p>Create results in your own validators with {@link #valid()} and
 * {@link #invalid(String)}. Read them from {@link ConfigValue#validate()},
 * {@link Config#validate()} or {@link ConfigManager#validateAll()}.
 */
public class ValidationResult {
  private final boolean valid;
  private final List<String> errors;

  private ValidationResult(boolean valid, List<String> errors) {
    this.valid = valid;
    this.errors = errors;
  }

  /**
   * Returns whether the checked value passed.
   *
   * @return {@code true} if valid; {@link #errors()} is then empty
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * Returns the error messages, in the order they were found. Don't modify the list; it may be
   * the result's own list.
   *
   * @return the messages; empty when {@link #isValid()} is {@code true}
   */
  public List<String> errors() {
    return errors;
  }

  /**
   * Returns all error messages joined with {@code ", "}, for logging or showing to a player.
   *
   * @return the joined messages; an empty string when {@link #isValid()} is {@code true}
   */
  public String errorMessage() {
    return String.join(", ", errors);
  }

  /**
   * Returns a passing result.
   *
   * @return a valid result with no errors
   */
  public static ValidationResult valid() {
    return new ValidationResult(true, Collections.emptyList());
  }

  /**
   * Returns a failing result with one message. Write the message for the server admin who
   * edits the file, such as {@code "Value must be between 1 and 300"}; the loader puts the
   * value's path in front of it.
   *
   * @param error what is wrong with the value
   * @return an invalid result
   */
  public static ValidationResult invalid(String error) {
    return new ValidationResult(false, List.of(error));
  }

  /**
   * Returns a failing result with several messages. The list is copied.
   *
   * @param errors what is wrong with the value; should not be empty, since an invalid result
   *     without messages gives the admin nothing to fix
   * @return an invalid result, even if {@code errors} is empty
   */
  public static ValidationResult invalid(List<String> errors) {
    return new ValidationResult(false, new ArrayList<>(errors));
  }

  /**
   * Combines results into one with the error messages of all failing results, in argument
   * order. The combined result is invalid if there is at least one message. An invalid result
   * created with an empty list has no messages, so it does not make the combined result
   * invalid.
   *
   * @param results the results to combine
   * @return the combined result; valid when called with no arguments
   */
  public static ValidationResult combine(ValidationResult... results) {
    List<String> allErrors = new ArrayList<>();
    for (ValidationResult result : results) {
      if (!result.isValid()) {
        allErrors.addAll(result.errors());
      }
    }
    return allErrors.isEmpty() ? valid() : invalid(allErrors);
  }
}
