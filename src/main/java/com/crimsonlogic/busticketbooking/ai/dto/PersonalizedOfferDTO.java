package com.crimsonlogic.busticketbooking.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedOfferDTO {
    private String offerCode;
    private String description;
    private String discountType; // PERCENTAGE or FIXED_AMOUNT
    private BigDecimal discountValue;
    private BigDecimal minimumBookingAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private boolean isUsed;
}
