package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.StopType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteStopCreateRequest {

    @NotBlank
    private String stopName;

    @NotNull
    private Integer stopSequence;

    @NotNull
    private StopType stopType;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal distanceFromSourceKm;
}