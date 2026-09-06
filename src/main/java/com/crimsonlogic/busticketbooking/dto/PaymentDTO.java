package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

    private String paymentId;

    private String transactionReference;

    private String gatewayTransactionId;

    private String paymentMethod;

    private PaymentStatus paymentStatus;

    private BigDecimal paymentAmount;

    private String failureReason;

    private LocalDateTime paymentInitiatedAt;

    private LocalDateTime paymentCompletedAt;

    private LocalDateTime refundInitiatedAt;

    private LocalDateTime refundCompletedAt;

    private String refundReference;

    private String bookingId;

    private String bookingReference;
}
