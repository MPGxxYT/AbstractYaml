package me.mortaldev.config2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a validation operation.
 */
public class ValidationResult {
  private final boolean valid;
  private final List<String> errors;

  private ValidationResult(boolean valid, List<String> errors) {
    this.valid = valid;
    this.errors = errors;
  }

  public boolean isValid() {
    return valid;
  }

  public List<String> errors() {
    return errors;
  }

  public String errorMessage() {
    return String.join(", ", errors);
  }

  public static ValidationResult valid() {
    return new ValidationResult(true, Collections.emptyList());
  }

  public static ValidationResult invalid(String error) {
    return new ValidationResult(false, List.of(error));
  }

  public static ValidationResult invalid(List<String> errors) {
    return new ValidationResult(false, new ArrayList<>(errors));
  }

  /**
   * Combines multiple validation results.
   * The combined result is valid only if all results are valid.
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
