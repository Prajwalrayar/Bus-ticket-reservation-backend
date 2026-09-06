package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    @Column(name = "preference_id", length = 30, updatable = false)
    private String preferenceId;

    /*
     * The user these preferences belong to.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /*
     * Example: "Morning", "Night", "18:00-22:00"
     */
    @Column(name = "preferred_departure_time", length = 50)
    private String preferredDepartureTime;

    /*
     * Example: "Volvo", "Scania", "Ordinary"
     */
    @Column(name = "preferred_bus_type", length = 50)
    private String preferredBusType;

    /*
     * Minimum preferred price range.
     */
    @Column(name = "min_price", precision = 10, scale = 2)
    private BigDecimal minPrice;

    /*
     * Maximum preferred price range.
     */
    @Column(name = "max_price", precision = 10, scale = 2)
    private BigDecimal maxPrice;

    /*
     * Example: "VRL Travels", "Orange Travels"
     */
    @Column(name = "operator_preference", length = 100)
    private String operatorPreference;

    /*
     * True if user prefers AC buses.
     */
    @Column(name = "ac_preference")
    private Boolean acPreference;

    /*
     * True if user prefers Sleeper buses.
     */
    @Column(name = "sleeper_preference")
    private Boolean sleeperPreference;

    /*
     * Comma-separated or generic preferred boarding points.
     */
    @Column(name = "preferred_boarding_points", length = 500)
    private String preferredBoardingPoints;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void generateId() {
        if (this.preferenceId == null) {
            this.preferenceId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_USER_PREFERENCE);
        }
    }
}
