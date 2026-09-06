package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import com.crimsonlogic.busticketbooking.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancellations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cancellation {

    @Id
    @Column(name = "cancellation_id")
    private String cancellationId;


    /*
     * Customer/support-facing cancellation reference.
     * Example: CAN-8F72KQ
     */
    @Column(
            name = "cancellation_reference",
            nullable = false,
            unique = true,
            length = 30
    )
    private String cancellationReference;

    /*
     * Reason provided for cancelling the booking.
     */
    @Column(
            name = "cancellation_reason",
            length = 500
    )
    private String cancellationReason;

    /*
     * User account that performed the cancellation.
     *
     * This can be:
     * - Passenger
     * - Admin
     * - Support Agent
     * - Bus Operator
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cancelled_by_user_id",
            nullable = false
    )
    private User cancelledByUser;

    /*
     * Amount charged as cancellation fee.
     */
    @Column(
            name = "cancellation_fee",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal cancellationFee = BigDecimal.ZERO;

    /*
     * Amount returned to the customer after cancellation.
     */
    @Column(
            name = "refund_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /*
     * Current state of the refund.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "refund_status",
            nullable = false,
            length = 30
    )
    private RefundStatus refundStatus;

    /*
     * Reference of the refund transaction returned
     * by the payment gateway.
     */
    @Column(
            name = "refund_reference",
            length = 100
    )
    private String refundReference;

    /*
     * Time at which cancellation was performed.
     */
    @Column(
            name = "cancelled_at",
            nullable = false
    )
    private LocalDateTime cancelledAt;

    /*
     * Time at which the refund was successfully completed.
     */
    @Column(name = "refund_completed_at")
    private LocalDateTime refundCompletedAt;

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

    /*
     * Each booking can have at most one cancellation record.
     */
    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "booking_id",
            nullable = false,
            unique = true
    )
    private Booking booking;

    @PrePersist
    protected void generateId() {
        if (this.cancellationId == null) {
            this.cancellationId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_CANCELLATION);
        }
    }
}
