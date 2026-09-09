package com.crimsonlogic.busticketbooking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripCreateRequest {

    @NotBlank
    private String busRegistrationNumber;

    @NotBlank
    private String source;

    @NotBlank
    private String destination;

    @NotNull
    private LocalDate travelDate;

    private LocalTime departureTime;

    private LocalDate arrivalDate;

    private LocalTime arrivalTime;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal baseFare;

    private java.util.Map<String, BigDecimal> stopFares;

    private java.util.Map<String, LocalTime> stopTimes;

    private java.util.Map<String, LocalDate> stopDates;
}