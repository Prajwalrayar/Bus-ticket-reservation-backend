package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import com.crimsonlogic.busticketbooking.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_seats",
        uniqueConstraints = {@UniqueConstraint(
                name = "uk_trip_seats_trip_bus_seat",
                        columnNames = {"trip_id", "bus_seat_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TripSeat {

    @Id
    @Column(name = "trip_seat_id")
    private String tripSeatId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "seat_status",
            nullable = false,
            length = 20
    )
    private SeatStatus seatStatus;


    /*
     * Fare applicable to this particular seat on this particular trip.
     *
     * Different seats can have different fares.
     * Example:
     * Single sleeper -> â‚¹1,200
     * Double sleeper -> â‚¹900
     */
    @Column(
            name = "seat_fare",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal seatFare;


    /*
     * Time until which a temporarily locked seat remains locked.
     * Null when the seat is not temporarily locked.
     */
    @Column(name = "lock_expiry_time")
    private LocalDateTime lockExpiryTime;

    @Column(name = "locked_by_user_id")
    private String lockedByUserId;

    /*
     * Optimistic locking for concurrent seat updates.
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
     * Each TripSeat belongs to exactly one trip.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "trip_id",
            nullable = false
    )
    private Trip trip;


    /*
     * Each TripSeat represents exactly one physical seat.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "bus_seat_id",
            nullable = false
    )
    private BusSeat busSeat;

    @PrePersist
    protected void generateId() {
        if (this.tripSeatId == null) {
            this.tripSeatId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_TRIP_SEAT);
        }
    }
}
