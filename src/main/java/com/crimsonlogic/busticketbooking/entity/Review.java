package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_booking_user",
                        columnNames = {"booking_id", "user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @Column(
            name = "review_id",
            length = 30,
            updatable = false
    )
    private String reviewId;

    /*
     * Rating given by the passenger.
     * Usually from 1 to 5.
     */
    @Column(
            name = "rating",
            nullable = false
    )
    private Integer rating;

    /*
     * Optional feedback provided by the passenger.
     */
    @Column(
            name = "comment",
            length = 1000
    )
    private String comment;

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
     * AI Sentiment Analysis Classification.
     * Stored as POSITIVE, NEUTRAL, or NEGATIVE.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_status", length = 20)
    private com.crimsonlogic.busticketbooking.enums.SentimentStatus sentimentStatus;

    /*
     * AI Sentiment Score (e.g., -1.0 to 1.0 or 0.0 to 1.0).
     */
    @Column(name = "sentiment_score")
    private Double sentimentScore;

    /*
     * User who submitted the review.
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

    /*
     * Booking on which the review is based.
     *
     * This also ensures that the passenger can review
     * a trip they actually booked.
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
        if (this.reviewId == null) {
            this.reviewId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_REVIEW);
        }
    }
}