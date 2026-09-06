package com.crimsonlogic.busticketbooking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            setErrorMessage(context, "Email is required.");
            return false;
        }

        String trimmedValue = value.trim();

        if (!EMAIL_PATTERN.matcher(trimmedValue).matches()) {
            setErrorMessage(context, "Please enter a valid email address.");
            return false;
        }

        String lower = trimmedValue.toLowerCase();
        if (lower.startsWith("aaa@") || lower.startsWith("abc@") || lower.startsWith("test@")) {
            setErrorMessage(context, "Please enter a valid email address.");
            return false;
        }

        return true;
    }

    private void setErrorMessage(ConstraintValidatorContext context, String message) {
        if (context != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                   .addConstraintViolation();
        }
    }
}
