package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteDTO {

    private String routeId;

    private String source;

    private String destination;

    private BigDecimal distance;

    private Boolean isActive;
}