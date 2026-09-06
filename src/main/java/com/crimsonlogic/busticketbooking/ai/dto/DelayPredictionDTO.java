package com.crimsonlogic.busticketbooking.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DelayPredictionDTO {
    private String tripId;
    private LocalTime scheduledArrivalTime;
    private LocalTime expectedArrivalTime;
    private Double delayProbability; // 0.0 to 1.0
    private String reasoning;
}
