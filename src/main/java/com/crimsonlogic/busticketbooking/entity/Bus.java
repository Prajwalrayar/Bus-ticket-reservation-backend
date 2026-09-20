package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import com.crimsonlogic.busticketbooking.enums.BusType;
import com.crimsonlogic.busticketbooking.enums.BusActivationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "buses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_buses_registration_number",
                columnNames = "registration_number")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bus {

    @Id
    private String busId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private Operator operator;

    @Column(name = "registration_number", nullable = false,
            unique = true, length = 15)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "bus_type",
            nullable = false,
            length = 20
    )
    private BusType busType;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "bus_amenities",
            joinColumns = @JoinColumn(name = "bus_id"),
            uniqueConstraints = {@UniqueConstraint(
                    name = "uk_bus_amenity", columnNames = {"bus_id", "amenity"})
            }
    )
    @Column(name = "amenities", nullable = false, length = 30)
    private Set<String> amenities = new HashSet<>();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "pets_allowed", nullable = false)
    private Boolean petsAllowed = false;

    @Column(name = "baggage_policy", length = 500)
    private String baggagePolicy;

    /** Date of the most recent trip operated by this bus. Updated on trip creation. */
    @Column(name = "last_trip_date")
    private LocalDate lastTripDate;

    /** Tracks whether an operator has submitted an activation request. */
    @Enumerated(EnumType.STRING)
    @Column(name = "activation_request_status", nullable = false, length = 20)
    private BusActivationStatus activationRequestStatus = BusActivationStatus.NONE;

    /** Operator's reason for requesting reactivation. */
    @Column(name = "activation_request_note", length = 1000)
    private String activationRequestNote;

    /** Compensation fee set by the admin upon approval. */
    @Column(name = "compensation_amount", precision = 10, scale = 2)
    private BigDecimal compensationAmount;

    /** Admin's note if the request is rejected. */
    @Column(name = "admin_rejection_note", length = 500)
    private String adminRejectionNote;

    @Column(name = "activation_requested_at")
    private LocalDateTime activationRequestedAt;

    @Column(name = "activation_approved_at")
    private LocalDateTime activationApprovedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void generateId() {
        if (this.busId == null) {
            this.busId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_BUS);
        }
    }

}
