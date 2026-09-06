package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_routes_source_destination",
                columnNames = {"source", "destination"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    private String routeId;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "destination", nullable = false, length = 100)
    private String destination;

    @Column( precision = 8, scale = 2,nullable = false)
    private BigDecimal distance;


    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /*
     * One route can contain multiple stops.
     * RouteStop owns the relationship.
     */
    @OneToMany(
            mappedBy = "route",
            fetch = FetchType.LAZY
    )
    private List<RouteStop> routeStops = new ArrayList<>();

    @PrePersist
    protected void generateId() {
        if (this.routeId == null) {
            this.routeId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_ROUTE);
        }
    }

}
