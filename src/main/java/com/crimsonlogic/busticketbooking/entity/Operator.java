package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "operators")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Operator {

    @Id
    @Column(name = "operator_id")
    private String operatorId;

    @Column(name = "company_name", nullable = false,unique = true, length = 100)
    private String companyName;

    @Column(name = "contact_email", nullable = false,unique = true, length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 15)
    private String contactPhone;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "website_url", length = 255)
    private String websiteUrl;

    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void generateId() {
        if (this.operatorId == null) {
            this.operatorId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_OPERATOR);
        }
    }
}
