package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import com.crimsonlogic.busticketbooking.enums.StopType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "route_stops", uniqueConstraints = {
        @UniqueConstraint(name = "uk_route_stops_route_type_sequence",
                columnNames = {"route_id", "stop_type", "stop_sequence"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteStop {

    @Id
    private String routeStopId;

    /*
     * Name of the boarding, intermediate, or dropping location.
     */
    @Column(name = "stop_name", nullable = false, length = 150)
    private String stopName;

    /*
     * Determines the order of this stop within the route.
     */
    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    /*
     * Defines the purpose of this stop on this directional route.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "stop_type",
            nullable = false,
            length = 20
    )
    private StopType stopType;

    /*
     * Distance of this stop from the route's source.
     */
    @Column(
            name = "distance_from_source_km",
            precision = 8,
            scale = 2,
            nullable = false
    )
    private BigDecimal distanceFromSourceKm;

    /*
     * Optional grouping of this physical stop into a logical fare zone.
     * Multiple physical stops can share the same FareLocation.
     * Null means no fare-zone grouping is configured for this stop.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "fare_location_id", nullable = true)
    private FareLocation fareLocation;

    /*
     * Whether passengers may board at this stop.
     * Defaults to true. The operator can disable boarding at a stop
     * without removing the stop from the route.
     */
    @Column(name = "can_board", nullable = false)
    private Boolean canBoard = true;

    /*
     * Whether passengers may alight at this stop.
     * Defaults to true.
     */
    @Column(name = "can_drop", nullable = false)
    private Boolean canDrop = true;

    /*
     * Each route stop belongs to exactly one route.
     * One route can contain multiple stops.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @PrePersist
    protected void generateId() {
        if (this.routeStopId == null) {
            this.routeStopId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_ROUTE_STOP);
        }
    }
}
