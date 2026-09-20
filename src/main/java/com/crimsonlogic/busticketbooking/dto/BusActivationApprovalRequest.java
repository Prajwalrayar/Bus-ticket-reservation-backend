package com.crimsonlogic.busticketbooking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BusActivationApprovalRequest {

    @NotNull(message = "Compensation amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Compensation must be >= 0")
    private BigDecimal compensationAmount;

    private String adminNote;
}
