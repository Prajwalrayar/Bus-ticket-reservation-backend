package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.StopType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteStopDTO {

    private String routeStopId;

    private String stopName;

    private Integer stopSequence;

    private StopType stopType;

    private BigDecimal distanceFromSourceKm;

    private String source;

    private String destination;
}