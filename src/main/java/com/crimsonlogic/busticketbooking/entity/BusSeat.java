package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import com.crimsonlogic.busticketbooking.enums.SeatPosition;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "bus_seats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bus_seats_bus_seat_number",
                columnNames = {"bus_id", "seat_number"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusSeat {

    @Id
    private String busSeatId;

    /*
     * Physical seat number/label shown to the passenger.
     * Examples: 1A, 1B, 2A, 2B.
     */
    @Column(
            name = "seat_number",
            nullable = false,
            length = 10
    )
    private String seatNumber;

    /*
     * Indicates whether the seat/berth is on the upper
     * or lower level of the bus.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "seat_position",
            nullable = false,
            length = 10
    )
    private SeatPosition seatPosition;


    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /*
     * Each physical seat belongs to exactly one bus.
     * One bus can have multiple physical seats.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bus_id",
            nullable = false
    )
    private Bus bus;

    @PrePersist
    protected void generateId() {
        if (this.busSeatId == null) {
            this.busSeatId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_BUS_SEAT);
        }
    }

}
