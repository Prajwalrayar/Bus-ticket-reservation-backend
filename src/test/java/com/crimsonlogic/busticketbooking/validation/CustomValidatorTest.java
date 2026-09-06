package com.crimsonlogic.busticketbooking.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomValidatorTest {

    private final ValidNameValidator nameValidator = new ValidNameValidator();
    private final ValidPasswordValidator passwordValidator = new ValidPasswordValidator();
    private final ValidEmailValidator emailValidator = new ValidEmailValidator();
    private final ValidIndianPhoneValidator phoneValidator = new ValidIndianPhoneValidator();

    // Mock Context
    private final jakarta.validation.ConstraintValidatorContext mockContext = null;

    @Test
    void testNameValidator() {
        // Valid
        assertTrue(nameValidator.isValid("John", mockContext));
        assertTrue(nameValidator.isValid("Rahul Kumar", mockContext));

        // Invalid - Dummies
        assertFalse(nameValidator.isValid("aaa", mockContext));
        assertFalse(nameValidator.isValid("abc", mockContext));
        assertFalse(nameValidator.isValid("test", mockContext));

        // Invalid - Length/Chars
        assertFalse(nameValidator.isValid("ab", mockContext)); // too short
        assertFalse(nameValidator.isValid("A1B", mockContext)); // contains number
        assertFalse(nameValidator.isValid("John123", mockContext)); // contains number
        assertFalse(nameValidator.isValid("@John", mockContext)); // contains special char
    }

    @Test
    void testPasswordValidator() {
        // Valid
        assertTrue(passwordValidator.isValid("Password@123", mockContext));
        assertTrue(passwordValidator.isValid("Welcome#12", mockContext));

        // Invalid
        assertFalse(passwordValidator.isValid("password", mockContext)); // missing uppercase, special, digit
        assertFalse(passwordValidator.isValid("Password", mockContext)); // missing special, digit
        assertFalse(passwordValidator.isValid("Password123", mockContext)); // missing special
        assertFalse(passwordValidator.isValid("password@123", mockContext)); // missing uppercase
        assertFalse(passwordValidator.isValid("PASSWORD@123", mockContext)); // missing lowercase
        assertFalse(passwordValidator.isValid("12345678", mockContext)); // missing letters/special
        assertFalse(passwordValidator.isValid("Abc@1", mockContext)); // too short
    }

    @Test
    void testEmailValidator() {
        // Valid
        assertTrue(emailValidator.isValid("user@gmail.com", mockContext));
        assertTrue(emailValidator.isValid("rahul.kumar@gmail.com", mockContext));

        // Invalid
        assertFalse(emailValidator.isValid("aaa", mockContext));
        assertFalse(emailValidator.isValid("abc", mockContext));
        assertFalse(emailValidator.isValid("aaa@gmail", mockContext));
        assertFalse(emailValidator.isValid("abc@", mockContext));
        assertFalse(emailValidator.isValid("@gmail.com", mockContext));
        assertFalse(emailValidator.isValid("test@gmail", mockContext));
        assertFalse(emailValidator.isValid("test@@gmail.com", mockContext));

        // Invalid Dummies
        assertFalse(emailValidator.isValid("aaa@gmail.com", mockContext));
        assertFalse(emailValidator.isValid("abc@gmail.com", mockContext));
    }

    @Test
    void testIndianPhoneValidator() {
        // Valid
        assertTrue(phoneValidator.isValid("9876543210", mockContext));
        assertTrue(phoneValidator.isValid("9123456789", mockContext));
        assertTrue(phoneValidator.isValid("8765432109", mockContext));
        assertTrue(phoneValidator.isValid("+919876543210", mockContext));
        assertTrue(phoneValidator.isValid("919876543210", mockContext)); // Normalizes to 10 digit

        // Invalid
        assertFalse(phoneValidator.isValid("1234567890", mockContext)); // wrong start
        assertFalse(phoneValidator.isValid("5123456789", mockContext)); // wrong start
        assertFalse(phoneValidator.isValid("987654321", mockContext)); // too short
        assertFalse(phoneValidator.isValid("98765432101", mockContext)); // too long
        assertFalse(phoneValidator.isValid("98765abc10", mockContext)); // chars
        assertFalse(phoneValidator.isValid("abcdefghij", mockContext)); // chars
    }
}
