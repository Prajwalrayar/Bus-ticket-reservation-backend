package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "user_roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "role_name"})
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserRole {

    @Id
    @Column(name = "user_role_id")
    private String userRoleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_name", nullable = false, length = 30)
    private String roleName;

    @PrePersist
    protected void generateId() {
        if (this.userRoleId == null) {
            this.userRoleId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_USER_ROLE);
        }
    }

}
