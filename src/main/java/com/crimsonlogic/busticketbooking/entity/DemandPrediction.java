package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "demand_predictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DemandPrediction {

    @Id
    private String predictionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "predicted_occupancy")
    private Double predictedOccupancy;

    @Column(name = "demand_classification", length = 20)
    private String demandClassification; // LOW, MEDIUM, HIGH

    @CreationTimestamp
    @Column(name = "predicted_at", nullable = false, updatable = false)
    private LocalDateTime predictedAt;

    @PrePersist
    protected void generateId() {
        if (this.predictionId == null) {
            this.predictionId = EntityIdGenerator.generateStatic("PRD-");
        }
    }
}
