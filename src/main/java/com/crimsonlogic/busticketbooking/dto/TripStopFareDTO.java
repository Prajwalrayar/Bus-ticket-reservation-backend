package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripStopFareDTO {

    private String routeStopId;
    
    private String stopName;
    
    private BigDecimal fareFromSource;
    
    private Integer stopSequence;
    
    private String stopType;
    
    private LocalTime stopTime;
    
    private LocalDate stopDate;

    private String fareLocationId;

    private String fareLocationName;

    private Boolean canBoard;

    private Boolean canDrop;
}
