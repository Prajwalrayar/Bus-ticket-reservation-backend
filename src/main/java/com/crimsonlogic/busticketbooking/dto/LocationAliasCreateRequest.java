package com.crimsonlogic.busticketbooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LocationAliasCreateRequest {
    @NotBlank(message = "Alias is required")
    private String alias;
}
