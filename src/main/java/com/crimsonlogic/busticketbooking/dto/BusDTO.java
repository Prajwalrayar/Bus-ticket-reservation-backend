package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.BusType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusDTO {

    private String busId;

    private String registrationNumber;

    private BusType busType;

    private Set<String> amenities;

    private Boolean isActive;

    private String operatorCompanyName;
}
