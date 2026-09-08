package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import com.crimsonlogic.busticketbooking.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_transaction_reference",
                columnNames = "transaction_reference")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @Column(name = "payment_id")
    private String paymentId;

    @Column(
            name = "transaction_reference",
            nullable = false,
            unique = true,
            length = 50
    )
    private String transactionReference;

    @Column(
            name = "gateway_transaction_id",
            length = 100
    )
    private String gatewayTransactionId;

    @Column(
            name = "payment_method",
            nullable = false,
            length = 30
    )
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_status",
            nullable = false,
            length = 30
    )
    private PaymentStatus paymentStatus;

    @Column(
            name = "payment_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal paymentAmount;

    @Column(
            name = "failure_reason",
            length = 500
    )
    private String failureReason;

    @Column(
            name = "failure_code",
            length = 30
    )
    private String failureCode;

    @Column(name = "payment_initiated_at")
    private LocalDateTime paymentInitiatedAt;

    @Column(name = "payment_completed_at")
    private LocalDateTime paymentCompletedAt;

    @Column(name = "refund_initiated_at")
    private LocalDateTime refundInitiatedAt;

    @Column(name = "refund_completed_at")
    private LocalDateTime refundCompletedAt;

    @Column(
            name = "refund_reference",
            length = 100
    )
    private String refundReference;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "wallet_amount_used", precision = 10, scale = 2)
    private BigDecimal walletAmountUsed = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @PrePersist
    protected void generateId() {
        if (this.paymentId == null) {
            this.paymentId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_PAYMENT);
        }
    }

}
