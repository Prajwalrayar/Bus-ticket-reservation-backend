package com.crimsonlogic.busticketbooking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class ValidIndianPhoneValidator implements ConstraintValidator<ValidIndianPhone, String> {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            setErrorMessage(context, "Phone number is required.");
            return false;
        }

        String trimmedValue = value.trim();
        
        // Strip out international code if it's explicitly passed, assuming the system normalizes to 10 digits
        if (trimmedValue.startsWith("+91")) {
            trimmedValue = trimmedValue.substring(3);
        } else if (trimmedValue.startsWith("91") && trimmedValue.length() == 12) {
            trimmedValue = trimmedValue.substring(2);
        }

        if (!PHONE_PATTERN.matcher(trimmedValue).matches()) {
            setErrorMessage(context, "Enter a valid Indian mobile number.");
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
