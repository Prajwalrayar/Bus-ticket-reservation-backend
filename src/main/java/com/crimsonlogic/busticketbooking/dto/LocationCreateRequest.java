package com.crimsonlogic.busticketbooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LocationCreateRequest {
    @NotBlank(message = "Location name is required")
    private String name;
}
