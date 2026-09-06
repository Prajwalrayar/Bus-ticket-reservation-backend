package com.crimsonlogic.busticketbooking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class ValidNameValidator implements ConstraintValidator<ValidName, String> {

    // Alphabetic and spaces only, 3 to 50 characters
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z ]{3,50}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            setErrorMessage(context, "Name is required.");
            return false;
        }

        String trimmedValue = value.trim();

        if (trimmedValue.length() < 3) {
            setErrorMessage(context, "Name must contain at least 3 characters.");
            return false;
        }

        if (trimmedValue.length() > 50) {
            setErrorMessage(context, "Name cannot exceed 50 characters.");
            return false;
        }

        if (!NAME_PATTERN.matcher(trimmedValue).matches()) {
            setErrorMessage(context, "Name can contain only letters and spaces.");
            return false;
        }

        // Check for dummy names (e.g. 'aaa', 'bbb')
        if (isRepeatedCharacters(trimmedValue)) {
            setErrorMessage(context, "Please enter a valid name.");
            return false;
        }

        // Check for obvious placeholders
        String lower = trimmedValue.toLowerCase();
        if (lower.equals("abc") || lower.equals("test") || lower.equals("dummy")) {
            setErrorMessage(context, "Please enter a valid name.");
            return false;
        }

        return true;
    }

    private boolean isRepeatedCharacters(String str) {
        // Exclude spaces for this check
        String noSpace = str.replace(" ", "").toLowerCase();
        if (noSpace.isEmpty()) return true;
        char firstChar = noSpace.charAt(0);
        for (int i = 1; i < noSpace.length(); i++) {
            if (noSpace.charAt(i) != firstChar) {
                return false; // Found a different character
            }
        }
        return true; // All characters are the same
    }

    private void setErrorMessage(ConstraintValidatorContext context, String message) {
        if (context != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                   .addConstraintViolation();
        }
    }
}
