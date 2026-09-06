package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @Column( name = "audit_log_id")
    private String auditLogId;

    /*
     * Type/name of the action performed.
     *
     * Examples:
     * CREATE
     * UPDATE
     * DELETE
     * LOGIN
     * LOGOUT
     * CANCEL_BOOKING
     * APPROVE_OPERATOR
     */
    @Column(
            name = "action",
            nullable = false,
            length = 50
    )
    private String action;

    /*
     * Entity/resource on which the action was performed.
     *
     * Examples:
     * User
     * Booking
     * Bus
     * Operator
     * Payment
     */
    @Column(
            name = "entity_name",
            nullable = false,
            length = 50
    )
    private String entityName;

    /*
     * Human/business reference of the affected record.
     *
     * Examples:
     * BK-7F82QX
     * TKT-91A7XZ
     * PAY-8F72KQ
     *
     * We use a business reference instead of making the
     * audit log dependent on an internal database ID.
     */
    @Column(
            name = "entity_reference",
            length = 100
    )
    private String entityReference;

    /*
     * Additional explanation of what happened.
     */
    @Column(
            name = "description",
            length = 1000
    )
    private String description;

    /*
     * User who performed the action.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id")
    private User performedByUser;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void generateId() {
        if (this.auditLogId == null) {
            this.auditLogId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_AUDIT_LOG);
        }
    }
}
