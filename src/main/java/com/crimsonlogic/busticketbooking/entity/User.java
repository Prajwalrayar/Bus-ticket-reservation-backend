package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users",
        indexes = {@Index(name = "idx_users_user_email", columnList = "user_email"),
        @Index(name = "idx_users_mobile_number", columnList = "mobile_number")
}
)
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @Column(name="user_id")
    private String userId;

    @Column(name = "user_name", nullable = false, length = 20)
    private String userName;

    @Column(name = "user_email",nullable = false, unique = true, length = 254)
    private String userEmail;

    @Column(name = "mobile_number", nullable = false, length = 10)
    private String mobileNumber;

    @Column(name = "user_password", nullable = false, length = 255)
    private String userPassword;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "is_temporary_password", nullable = false)
    private Boolean isTemporaryPassword = false;


    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    /*
     * One User can have multiple role assignments.
     *
     * UserRole is the association entity between User and Role.
     */
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<UserRole> userRoles = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = true)
    private Operator operator;

    @PrePersist
    protected void onCreate() {
        if (this.userId == null) {
            // ID should be set by AuthService using role-based prefix.
            // This static fallback (PREFIX_PASSENGER) is a safety net only.
            this.userId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_PASSENGER);
        }
        LocalDateTime currentDateTime = LocalDateTime.now();
        createdAt = currentDateTime;
        updatedAt = currentDateTime;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addUserRole(UserRole userRole) {
        userRoles.add(userRole);
        userRole.setUser(this);
    }

    public void removeUserRole(UserRole userRole) {
        userRoles.remove(userRole);
        userRole.setUser(null);
    }
}
