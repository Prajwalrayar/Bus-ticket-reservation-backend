package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @Column(name = "ticket_id")
    private String ticketId;

    /*
     * Customer-facing ticket number.
     * Example: TKT-91A7XZ
     */
    @Column(
            name = "ticket_number",
            nullable = false,
            unique = true,
            length = 30
    )
    private String ticketNumber;


    /*
     * Time at which the ticket was issued.
     */
    @Column(
            name = "issued_at",
            nullable = false
    )
    private LocalDateTime issuedAt;

    /*
     * Value encoded inside the QR code/barcode.
     * The QR image itself does not need to be stored in the database.
     */
    @Column(
            name = "verification_code",
            nullable = false,
            unique = true,
            length = 100
    )
    private String verificationCode;

    /*
     * Time at which the ticket was validated/used,
     * if it has been validated.
     */
    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    /*
     * Staff user who validated the ticket.
     *
     * Normally this would be an authorized operator/admin
     * responsible for checking the passenger's ticket.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by_user_id")
    private User validatedByUser;

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

    /*
     * Each ticket belongs to exactly one booking.
     */
    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "booking_id",
            nullable = false,
            unique = true
    )
    private Booking booking;

    @PrePersist
    protected void generateId() {
        if (this.ticketId == null) {
            this.ticketId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_TICKET);
        }
    }
}
