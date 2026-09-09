package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FareLocationDTO {

    private String fareLocationId;

    private String name;

    private String description;

    private String routeId;

    private String source;

    private String destination;
}
