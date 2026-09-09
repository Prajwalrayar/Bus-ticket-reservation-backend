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

@Entity
@Table(name = "trip_segments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_trip_segment", columnNames = {"trip_id", "boarding_stop_id", "dropping_stop_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TripSegment {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boarding_stop_id", nullable = false)
    private RouteStop boardingStop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dropping_stop_id", nullable = false)
    private RouteStop droppingStop;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal fare;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void generateId() {
        if (this.id == null) {
            this.id = EntityIdGenerator.generateStatic("TSG");
        }
    }
}
