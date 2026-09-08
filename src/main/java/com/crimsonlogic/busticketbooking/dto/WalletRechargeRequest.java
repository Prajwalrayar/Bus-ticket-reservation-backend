package com.crimsonlogic.busticketbooking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletRechargeRequest {

    @NotNull(message = "Amount is required")
    @Min(value = 100, message = "Minimum recharge amount is Rs 100")
    private BigDecimal amount;

    @NotBlank(message = "UPI ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$", message = "Invalid UPI ID format")
    private String upiId;
}
