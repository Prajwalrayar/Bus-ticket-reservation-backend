package com.crimsonlogic.busticketbooking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            setErrorMessage(context, "Password is required.");
            return false;
        }

        if (value.length() < 8 || value.length() > 20) {
            setErrorMessage(context, "Password must be 8 to 20 characters.");
            return false;
        }

        if (!value.matches(".*[A-Z].*")) {
            setErrorMessage(context, "Password must contain at least 1 uppercase letter.");
            return false;
        }

        if (!value.matches(".*[a-z].*")) {
            setErrorMessage(context, "Password must contain at least 1 lowercase letter.");
            return false;
        }

        if (!value.matches(".*\\d.*")) {
            setErrorMessage(context, "Password must contain at least 1 digit.");
            return false;
        }

        if (!value.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>/?\\\\].*")) {
            setErrorMessage(context, "Password must contain at least 1 special character.");
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
