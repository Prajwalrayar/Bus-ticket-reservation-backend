package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteFareDTO {

    private String routeFareId;

    private String routeId;

    private String fromFareLocationId;

    private String fromFareLocationName;

    private String toFareLocationId;

    private String toFareLocationName;

    private BigDecimal fare;
}
