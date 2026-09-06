package com.crimsonlogic.busticketbooking.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

@Slf4j
@Component
public class PasswordUtil {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String DIGITS = "0123456789";
    private static final int PASSWORD_LENGTH = 10;
    private static final int OTP_LENGTH = 6;

    private final Random random = new SecureRandom();

    public String generateRandomPassword() {
        log.debug("Generating random password of length: {}", PASSWORD_LENGTH);
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return password.toString();
    }

    public String generateOTP() {
        log.debug("Generating {} digit OTP", OTP_LENGTH);
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }
        return otp.toString();
    }

    public String maskEmail(String email) {
        log.debug("Masking email: {}", email);
        if (email == null || !email.contains("@")) {
            return email;
        }

        int atIndex = email.indexOf("@");
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 2) {
            return username.charAt(0) + "***" + domain;
        }

        String maskedUsername = username.charAt(0)
                + "*".repeat(username.length() - 2)
                + username.charAt(username.length() - 1);

        return maskedUsername + domain;
    }
}
