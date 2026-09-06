package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.BusType;
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
public class TripSearchRequest {

    @NotBlank
    private String source;

    @NotBlank
    private String destination;

    private LocalDate travelDate;

    private BusType busType;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private LocalTime departureStart;

    private LocalTime departureEnd;

    private Boolean isAc;
}