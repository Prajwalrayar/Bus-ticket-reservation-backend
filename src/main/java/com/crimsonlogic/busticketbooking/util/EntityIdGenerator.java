package com.crimsonlogic.busticketbooking.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.function.Predicate;

/**
 * Centralized entity ID generator.
 *
 * Generates human-readable business IDs in the format:
 *   PREFIX + 6 random digits
 *
 * Examples: P482915, BUS928413, BK291847
 *
 * Two usage patterns:
 *
 *  1. Service layer (preferred, full collision safety):
 *     idGenerator.generate(PREFIX, repo::existsById)
 *
 *  2. Entity @PrePersist (no repo available):
 *     EntityIdGenerator.generateStatic(PREFIX)
 *     — DB primary-key constraint acts as the final uniqueness guarantee.
 *
 * IDs are always generated server-side; never trust client-provided IDs
 * as authoritative identifiers.
 */
@Slf4j
@Component
public class EntityIdGenerator {

    private static final int DIGIT_LENGTH = 6;
    private static final int BOUND        = 1_000_000; // 10^6
    private static final int MAX_RETRIES  = 20;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ── User prefixes (role-based) ────────────────────────────────
    public static final String PREFIX_ADMIN           = "A";
    public static final String PREFIX_PASSENGER       = "P";
    public static final String PREFIX_BUS_OPERATOR    = "B";
    public static final String PREFIX_SUPPORT_AGENT   = "S";

    // ── Core business entities ────────────────────────────────────
    public static final String PREFIX_OPERATOR        = "OP";
    public static final String PREFIX_BUS             = "BUS";
    public static final String PREFIX_BUS_SEAT        = "BS";
    public static final String PREFIX_ROUTE           = "RT";
    public static final String PREFIX_ROUTE_STOP      = "RS";
    public static final String PREFIX_TRIP            = "TR";
    public static final String PREFIX_TRIP_SEAT       = "TS";

    // ── Booking / payment / ticket ────────────────────────────────
    public static final String PREFIX_BOOKING         = "BK";
    public static final String PREFIX_BOOKING_SEAT    = "BST";
    public static final String PREFIX_PAYMENT         = "PAY";
    public static final String PREFIX_TICKET          = "TKT";
    public static final String PREFIX_CANCELLATION    = "CX";

    // ── Supporting entities ───────────────────────────────────────
    public static final String PREFIX_OFFER           = "OFR";
    public static final String PREFIX_REVIEW          = "RV";
    public static final String PREFIX_NOTIFICATION    = "NTF";
    public static final String PREFIX_SAVED_PASSENGER = "SP";
    public static final String PREFIX_WALLET          = "WL";
    public static final String PREFIX_SEARCH_HISTORY  = "SH";
    public static final String PREFIX_USER_ROLE       = "UR";
    public static final String PREFIX_USER_SECURITY   = "US";
    public static final String PREFIX_AUDIT_LOG       = "AL";
    public static final String PREFIX_PASSWORD_RESET  = "PR";
    public static final String PREFIX_USER_PREFERENCE = "UP";

    // ── Instance method: with repo-based uniqueness check ─────────

    /**
     * Generate a unique ID with the given prefix.
     * Retries up to MAX_RETRIES times if the candidate already exists.
     *
     * @param prefix      entity-specific prefix (e.g. "BK", "P")
     * @param existsCheck predicate returning true when the ID already exists
     * @return unique ID of the form prefix + 6 random digits
     */
    public String generate(String prefix, Predicate<String> existsCheck) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            String candidate = buildId(prefix);
            if (!existsCheck.test(candidate)) {
                log.debug("Generated ID '{}' on attempt {}", candidate, attempt);
                return candidate;
            }
            log.warn("ID collision on '{}' (attempt {}), retrying...", candidate, attempt);
        }
        throw new IllegalStateException(
                "Could not generate a unique ID for prefix '" + prefix + "' after " + MAX_RETRIES + " attempts."
        );
    }

    // ── Static method: for @PrePersist (no repo available) ────────

    /**
     * Generate an ID without a repository uniqueness check.
     * Intended for entity @PrePersist callbacks where Spring beans are
     * unavailable. The database primary-key constraint serves as the
     * final uniqueness guarantee.
     *
     * @param prefix entity-specific prefix
     * @return ID of the form prefix + 6 random digits
     */
    public static String generateStatic(String prefix) {
        return buildId(prefix);
    }

    // ── Role-based user prefix resolution ────────────────────────

    /**
     * Resolve the user ID prefix based on a role name string.
     *
     * @param roleName role string (e.g. "ADMIN", "PASSENGER")
     * @return the corresponding user ID prefix
     */
    public static String userPrefixForRole(String roleName) {
        if (roleName == null) return PREFIX_PASSENGER;
        return switch (roleName.toUpperCase()) {
            case "ADMIN"         -> PREFIX_ADMIN;
            case "BUS_OPERATOR"  -> PREFIX_BUS_OPERATOR;
            case "SUPPORT_AGENT" -> PREFIX_SUPPORT_AGENT;
            default              -> PREFIX_PASSENGER;
        };
    }

    // ── Private helpers ───────────────────────────────────────────

    private static String buildId(String prefix) {
        int digits = SECURE_RANDOM.nextInt(BOUND);
        return prefix + String.format("%0" + DIGIT_LENGTH + "d", digits);
    }
}
