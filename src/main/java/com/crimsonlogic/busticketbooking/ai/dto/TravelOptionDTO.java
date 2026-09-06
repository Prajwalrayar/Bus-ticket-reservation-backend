package com.crimsonlogic.busticketbooking.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import com.crimsonlogic.busticketbooking.dto.TripDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelOptionDTO {
    private List<TripDTO> trips;
    private BigDecimal totalPrice;
    private Double totalDurationHours;
    private Double score;
    private String optionType; // "DIRECT" or "CONNECTING"
}
