package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationEstimateDTO {
    private String bookingId;
    private BigDecimal totalAmount;
    private BigDecimal cancellationFeePercentage;
    private BigDecimal cancellationFee;
    private BigDecimal refundAmount;
    private String ruleApplied;
}
