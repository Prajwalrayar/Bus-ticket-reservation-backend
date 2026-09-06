package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.enums.SupportTicketStatus;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
public class SupportTicket {

    @Id
    @Column(name = "ticket_id")
    private String ticketId;

    @Column(name = "issue_subject", nullable = false, length = 100)
    private String issueSubject;

    @Column(name = "issue_description", nullable = false, length = 1000)
    private String issueDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    @Column(name = "booking_reference", length = 50)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.ticketId == null) {
            this.ticketId = EntityIdGenerator.generateStatic("TKT");
        }
    }
}
