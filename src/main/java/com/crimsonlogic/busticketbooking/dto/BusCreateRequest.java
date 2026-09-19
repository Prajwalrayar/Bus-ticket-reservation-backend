package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.BusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotNull(message = "Operator company name cannot be null")
    @Size(min = 2, max = 100, message = "Operator company name must be between 2 and 100 characters")
    private String operatorCompanyName;

    private Boolean petsAllowed = false;

    @Size(max = 500, message = "Baggage policy cannot exceed 500 characters")
    private String baggagePolicy;
}
