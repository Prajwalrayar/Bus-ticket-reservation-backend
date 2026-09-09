package com.crimsonlogic.busticketbooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TripSegmentCreateRequest {
    @NotBlank(message = "Boarding stop ID is required")
    private String boardingStopId;

    @NotBlank(message = "Dropping stop ID is required")
    private String droppingStopId;

    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalTime arrivalTime;

    private LocalDate departureDate;
    private LocalDate arrivalDate;

    @NotNull(message = "Fare is required")
    private BigDecimal fare;
}
