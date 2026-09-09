package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A FareLocation represents a logical fare zone on a route.
 *
 * Multiple physical boarding/dropping stops can be grouped under the
 * same FareLocation so that the system resolves fares at the
 * FareLocation → FareLocation level, not stop → stop.
 *
 * Example:
 *   Route: Bangalore → Mysore
 *   FareLocation "Bangalore" → stops: Majestic, Jalahalli, Yeshwanthpur
 *   FareLocation "Mysore"    → stops: Mysore City Bus Stand, KR Circle
 */
@Entity
@Table(
        name = "fare_locations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_fare_location_route_name",
                        columnNames = {"route_id", "name"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FareLocation {

    @Id
    @Column(name = "fare_location_id")
    private String fareLocationId;

    /*
     * Display name of this fare zone (e.g. "Bangalore", "Mysore").
     */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /*
     * Optional human-readable description of this fare zone.
     */
    @Column(name = "description", length = 255)
    private String description;

    /*
     * The route this fare location belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    /*
     * Physical stops that are grouped under this fare zone.
     * (Informational back-reference; owned by RouteStop.)
     */
    @OneToMany(mappedBy = "fareLocation", fetch = FetchType.LAZY)
    private List<RouteStop> routeStops = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void generateId() {
        if (this.fareLocationId == null) {
            this.fareLocationId = EntityIdGenerator.generateStatic("FL");
        }
    }
}
