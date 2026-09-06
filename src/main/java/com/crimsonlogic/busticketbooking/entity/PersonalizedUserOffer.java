package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "personalized_user_offers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "offer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedUserOffer {

    @Id
    @Column(name = "user_offer_id", length = 30, updatable = false)
    private String userOfferId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void generateId() {
        if (this.userOfferId == null) {
            this.userOfferId = EntityIdGenerator.generateStatic("PUO-");
        }
    }
}
