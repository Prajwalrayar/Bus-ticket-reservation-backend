package com.crimsonlogic.busticketbooking.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DynamicPricingDTO {
    private String tripId;
    private BigDecimal baseFare;
    private BigDecimal simulatedFare;
    private String demandClassification;
    private int availableSeats;
    private boolean scarcityApplied;
    private String reasoning;
}
