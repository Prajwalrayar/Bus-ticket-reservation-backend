package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset")
@Getter
@Setter
@NoArgsConstructor
public class PasswordReset {

    @Id
    @Column(name = "password_reset_id")
    private String passwordResetId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "reset_password_token", unique = true, length = 255)
    private String resetPasswordToken;

    @Column(name = "reset_token_expiry_time")
    private LocalDateTime resetTokenExpiryTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.passwordResetId == null) {
            this.passwordResetId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_PASSWORD_RESET);
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
