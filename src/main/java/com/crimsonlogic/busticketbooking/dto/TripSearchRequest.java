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

    private String source;
    private String destination;

    private Long fromLocationId;
    private Long toLocationId;

    private LocalDate travelDate;

    private BusType busType;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private LocalTime departureStart;

    private LocalTime departureEnd;

    private Boolean isAc;
}