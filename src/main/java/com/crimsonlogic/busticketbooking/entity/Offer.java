package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import com.crimsonlogic.busticketbooking.enums.DiscountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Offer {

    @Id
    @Column(
            name = "offer_id",
            length = 30,
            updatable = false
    )
    private String offerId;

    /*
     * Code entered by the customer during booking.
     * Example: FIRSTBUS10
     */
    @Column(
            name = "offer_code",
            nullable = false,
            unique = true,
            length = 30
    )
    private String offerCode;

    /*
     * Offer description shown to customers.
     */
    @Column(
            name = "description",
            length = 500
    )
    private String description;

    /*
     * Determines whether the discount is percentage-based
     * or a fixed amount.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "discount_type",
            nullable = false,
            length = 20
    )
    private DiscountType discountType;

    /*
     * Discount value.
     *
     * Example:
     * PERCENTAGE    â†’ 10.00 means 10%
     * FIXED_AMOUNT  â†’ 200.00 means â‚¹200
     */
    @Column(
            name = "discount_value",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal discountValue;

    /*
     * Minimum booking amount required to use the offer.
     */
    @Column(
            name = "minimum_booking_amount",
            precision = 10,
            scale = 2
    )
    private BigDecimal minimumBookingAmount = BigDecimal.ZERO;

    /*
     * Maximum discount allowed for percentage-based offers.
     *
     * Example:
     * 20% discount, maximum â‚¹500.
     */
    @Column(
            name = "maximum_discount_amount",
            precision = 10,
            scale = 2
    )
    private BigDecimal maximumDiscountAmount;

    /*
     * Date and time from which the offer becomes valid.
     */
    @Column(
            name = "valid_from",
            nullable = false
    )
    private LocalDateTime validFrom;

    /*
     * Date and time after which the offer can no longer be used.
     */
    @Column(
            name = "valid_until",
            nullable = false
    )
    private LocalDateTime validUntil;

    /*
     * Whether the offer is currently enabled by the administrator.
     */
    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean isActive = true;

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

    @PrePersist
    protected void generateId() {
        if (this.offerId == null) {
            this.offerId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_OFFER);
        }
    }
}