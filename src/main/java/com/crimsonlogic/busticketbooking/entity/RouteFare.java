package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RouteFare stores the base passenger fare between two FareLocations on a route.
 *
 * Fares are always resolved FareLocation → FareLocation.
 * Physical stops are never directly priced against each other.
 *
 * Example:
 *   Route: Bangalore → Mysore
 *   RouteFare: FareLocation("Bangalore") → FareLocation("Mysore") = ₹350
 *
 * An administrator manually configures these fares.
 */
@Entity
@Table(
        name = "route_fares",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_route_fare_from_to",
                        columnNames = {"route_id", "from_fare_location_id", "to_fare_location_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteFare {

    @Id
    @Column(name = "route_fare_id")
    private String routeFareId;

    /*
     * The route this fare configuration belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    /*
     * The originating fare zone of the passenger.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_fare_location_id", nullable = false)
    private FareLocation fromFareLocation;

    /*
     * The destination fare zone of the passenger.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_fare_location_id", nullable = false)
    private FareLocation toFareLocation;

    /*
     * The configured fare for this route segment.
     */
    @Column(name = "fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void generateId() {
        if (this.routeFareId == null) {
            this.routeFareId = EntityIdGenerator.generateStatic("RF");
        }
    }
}
