package com.crimsonlogic.busticketbooking.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandPredictionDTO {
    private String tripId;
    private Double predictedOccupancy; // e.g., 0.85 for 85%
    private String demandClassification; // "LOW", "MEDIUM", "HIGH"
    private LocalDateTime predictedAt;
}
