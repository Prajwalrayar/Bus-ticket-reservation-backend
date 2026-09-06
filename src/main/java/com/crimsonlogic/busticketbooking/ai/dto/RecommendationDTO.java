package com.crimsonlogic.busticketbooking.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.crimsonlogic.busticketbooking.dto.TripDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDTO {
    private TripDTO trip;
    private Double score;
    private String recommendationReason;
}
