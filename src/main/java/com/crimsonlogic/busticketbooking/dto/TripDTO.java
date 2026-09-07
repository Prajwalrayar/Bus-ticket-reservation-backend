package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripDTO {

    // ── Core trip fields ─────────────────────────────────────
    private String tripId;

    private LocalDate travelDate;

    private LocalTime departureTime;

    private LocalDate arrivalDate;

    private LocalTime arrivalTime;

    private BigDecimal baseFare;

    private List<TripStopFareDTO> stopFares;

    private Boolean isCancelled;

    private String cancellationReason;

    // ── Route fields ─────────────────────────────────────────
    private String source;

    private String destination;

    // ── Bus fields ───────────────────────────────────────────
    private String busRegistrationNumber;

    /** BusType enum value as string: e.g. AC_SLEEPER, NON_AC_SEATER */
    private String busType;

    private Set<String> amenities;

    // ── Operator fields ──────────────────────────────────────
    private String operatorName;

    // ── Seat availability (computed) ─────────────────────────
    private int totalSeats;

    private int availableSeats;
}