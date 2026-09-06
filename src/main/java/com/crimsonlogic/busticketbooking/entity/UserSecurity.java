package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_security")
@Getter
@Setter
@NoArgsConstructor
public class UserSecurity {

    @Id
    @Column(name = "security_id")
    private String securityId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "login_attempts", nullable = false)
    private Integer loginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.securityId == null) {
            this.securityId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_USER_SECURITY);
        }
        LocalDateTime currentDateTime = LocalDateTime.now();
        createdAt = currentDateTime;
        updatedAt = currentDateTime;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
