package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trips", uniqueConstraints = {@UniqueConstraint(
                name = "uk_trips_trip_code", columnNames = "trip_code")
        }, indexes = {
        @Index(
                name = "idx_trips_route_travel_date",
                        columnList = "route_id, travel_date"),
        @Index(name = "idx_trips_bus_travel_date",
                        columnList = "bus_id, travel_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

    @Id
    @Column(name = "trip_id")
    private String tripId;

    /*
     * Date on which this journey operates.
     */
    @Column(
            name = "travel_date",
            nullable = false
    )
    private LocalDate travelDate;

    /*
     * Scheduled departure time from the source.
     */
    @Column(
            name = "departure_time",
            nullable = false
    )
    private LocalTime departureTime;

    /*
     * Scheduled arrival time at the destination.
     */
    @Column(
            name = "arrival_time",
            nullable = false
    )
    private LocalTime arrivalTime;

    /*
     * Date on which this journey arrives at the destination.
     */
    @Column(
            name = "arrival_date",
            nullable = false
    )
    private LocalDate arrivalDate;

    /*
     * Base fare for this particular trip.
     */
    @Column(
            name = "base_fare",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal baseFare;


    /*
     * Indicates that this scheduled trip has been cancelled.
     */
    @Column(
            name = "is_cancelled",
            nullable = false
    )
    private Boolean isCancelled = false;


    /*
     * Reason recorded when the trip is cancelled.
     */
    @Column(
            name = "cancellation_reason",
            length = 500
    )
    private String cancellationReason;

    /*
     * Optimistic locking for concurrent updates.
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
     * Each trip uses exactly one bus.
     * One bus can operate multiple trips.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "bus_id",
            nullable = false
    )
    private Bus bus;

    /*
     * Each trip follows exactly one route.
     * One route can have multiple trips.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "route_id",
            nullable = false
    )
    private Route route;

    /*
     * A trip has trip-specific seat availability.
     */
    @OneToMany(
            mappedBy = "trip",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<TripSeat> tripSeats = new ArrayList<>();

    @PrePersist
    protected void generateId() {
        if (this.tripId == null) {
            this.tripId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_TRIP);
        }
    }

}
