package com.crimsonlogic.busticketbooking.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TripSegmentDTO {
    private String id;
    private String tripId;
    
    private String boardingStopId;
    private String boardingStopName;
    private String boardingZoneName;
    private LocalTime departureTime;
    private LocalDate departureDate;
    
    private String droppingStopId;
    private String droppingStopName;
    private String droppingZoneName;
    private LocalTime arrivalTime;
    private LocalDate arrivalDate;
    
    private BigDecimal fare;
}
