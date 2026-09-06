package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancellationDTO {

    private String cancellationId;

    private String cancellationReference;

    private String cancellationReason;

    private BigDecimal cancellationFee;

    private BigDecimal refundAmount;

    private RefundStatus refundStatus;

    private String refundReference;

    private LocalDateTime cancelledAt;

    private LocalDateTime refundCompletedAt;

    private String bookingId;

    private String cancelledByUserId;
}