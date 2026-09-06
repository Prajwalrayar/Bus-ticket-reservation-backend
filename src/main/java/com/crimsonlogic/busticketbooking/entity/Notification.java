package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import com.crimsonlogic.busticketbooking.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @Column(
            name = "notification_id")
    private String notificationId;

    /*
     * Type of notification.
     *
     * Examples:
     * BOOKING_CONFIRMED
     * PAYMENT_SUCCESS
     * TICKET_ISSUED
     * BOOKING_CANCELLED
     * REFUND_COMPLETED
     * TRIP_REMINDER
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "notification_type",
            nullable = false,
            length = 40
    )
    private NotificationType notificationType;


    /*
     * Notification heading.
     */
    @Column(
            name = "title",
            nullable = false,
            length = 150
    )
    private String title;

    /*
     * Actual notification message.
     */
    @Column(
            name = "message",
            nullable = false,
            length = 1000
    )
    private String message;

    /*
     * Whether the user has read the notification.
     */
    @Column(
            name = "is_read",
            nullable = false
    )
    private Boolean isRead = false;

    /*
     * Time at which the notification was read.
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /*
     * User who receives this notification.
     */
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
        if (this.notificationId == null) {
            this.notificationId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_NOTIFICATION);
        }
    }
}
