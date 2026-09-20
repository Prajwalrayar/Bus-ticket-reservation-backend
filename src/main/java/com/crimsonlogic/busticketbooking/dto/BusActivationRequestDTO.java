package com.crimsonlogic.busticketbooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BusActivationRequestDTO {

    @NotBlank(message = "Reason is required")
    private String reason;
}
