package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import com.crimsonlogic.busticketbooking.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @Column(name = "booking_id")
    private String bookingId;

    /*
     * Customer-facing reference used to identify the booking.
     * Example: BK-7F82QX
     */
    @Column(
            name = "booking_reference",
            nullable = false,
            unique = true,
            length = 30
    )
    private String bookingReference;

    /*
     * Current state of the booking.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "booking_status",
            nullable = false,
            length = 20
    )
    private BookingStatus bookingStatus;

    /*
     * Total number of seats included in this booking.
     *
     * This is stored because it represents the booking
     * snapshot at the time of booking.
     */
    @Column(
            name = "total_seats",
            nullable = false
    )
    private Integer totalSeats;

    /*
     * Sum of the fares of all seats selected for this booking.
     */
    @Column(
            name = "base_fare_total",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal baseFareTotal;

    /*
     * Discount actually applied to this booking.
     */
    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /*
     * Tax actually charged for this booking.
     */
    @Column(
            name = "tax_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /*
     * Optional insurance amount charged for this booking.
     */
    @Column(
            name = "insurance_amount",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal insuranceAmount = BigDecimal.ZERO;


    /*
     * Total amount payable for this booking.
     */
    @Column(
            name = "total_amount",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal totalAmount;

    /*
     * Boarding point selected by the passenger.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "boarding_point_id",
            nullable = false
    )
    private RouteStop boardingPoint;

    /*
     * Dropping point selected by the passenger.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "dropping_point_id",
            nullable = false
    )
    private RouteStop droppingPoint;


    @Column(name = "offer_code_used", length = 30)
    private String offerCodeUsed;


    @Column(name = "cancellation_deadline")
    private LocalDateTime cancellationDeadline;

    @Column(name = "expiry_time")
    private LocalDateTime expiryTime;


    /*
     * Optimistic locking for concurrent booking updates.
     */
    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /*
     * User account that created/performed the booking.
     *
     * This can be:
     * - the passenger booking for themselves
     * - a support agent booking on behalf of a passenger
     * - a bus operator making a counter booking
     * - an admin making a booking
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "booked_by_user_id",
            nullable = false
    )
    private User bookedByUser;

    /*
     * The trip for which this booking was created.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "trip_id",
            nullable = false
    )
    private Trip trip;
    /*
     * Seats included in this booking.
     */
    @OneToMany(
            mappedBy = "booking",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<BookingSeat> bookingSeats = new ArrayList<>();

    /*
     * Payment records associated with this booking.
     */
    @OneToMany(
            mappedBy = "booking",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Payment> payments = new ArrayList<>();

    /*
     * Ticket generated for a confirmed booking.
     */
    @OneToOne(
            mappedBy = "booking",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Ticket ticket;

    /*
     * Cancellation information, if the booking is cancelled.
     */
    @OneToOne(
            mappedBy = "booking",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Cancellation cancellation;

    @PrePersist
    protected void generateId() {
        if (this.bookingId == null) {
            this.bookingId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_BOOKING);
        }
    }
}
