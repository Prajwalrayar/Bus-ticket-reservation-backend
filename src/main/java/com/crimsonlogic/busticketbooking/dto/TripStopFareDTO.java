package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripStopFareDTO {

    private String routeStopId;
    
    private String stopName;
    
    private BigDecimal fareFromSource;
    
    private Integer stopSequence;
    
    private String stopType;
}
