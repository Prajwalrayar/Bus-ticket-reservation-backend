package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_passengers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_saved_passenger_user_name_contact",
                columnNames = {"user_id", "passenger_name", "contact_number"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavedPassenger {

    @Id
    @Column(name = "saved_passenger_id")
    private String savedPassengerId;

    @Column(
            name = "passenger_name",
            nullable = false,
            length = 100
    )
    private String passengerName;

    @Column(
            name = "age",
            nullable = false
    )
    private Integer age;

    @Column(
            name = "gender",
            nullable = false,
            length = 15
    )
    private String gender;

    @Column(
            name = "id_type",
            length = 30
    )
    private String idType;

    @Column(
            name = "id_number",
            length = 30
    )
    private String idNumber;

    @Column(
            name = "contact_number",
            length = 15
    )
    private String contactNumber;

    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @PrePersist
    protected void generateId() {
        if (this.savedPassengerId == null) {
            this.savedPassengerId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_SAVED_PASSENGER);
        }
    }
}
