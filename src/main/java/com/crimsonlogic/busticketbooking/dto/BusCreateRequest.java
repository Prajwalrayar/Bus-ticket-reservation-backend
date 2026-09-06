package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.BusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusCreateRequest {

    @NotBlank
    private String registrationNumber;

    @NotNull
    private BusType busType;

    private Set<String> amenities;

    @NotBlank
    private String operatorCompanyName;
}
