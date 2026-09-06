package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "booking_seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeat {

    @Id
    @Column(name = "booking_seat_id")
    private String bookingSeatId;

    @Column(
            name = "passenger_name",
            nullable = false,
            length = 100
    )
    private String passengerName;

    @Column(
            name = "passenger_age",
            nullable = false
    )
    private Integer passengerAge;

    @Column(
            name = "passenger_gender",
            nullable = false,
            length = 15
    )
    private String passengerGender;

    @Column(
            name = "id_type",
            length = 30
    )
    private String idType;

    @Column(
            name = "id_number",
            length = 30
    )
    private String idNumber;

    @Column(
            name = "contact_number",
            length = 15
    )
    private String contactNumber;

    @Column(
            name = "seat_fare",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal seatFare;

    @Column(
            name = "is_primary",
            nullable = false
    )
    private Boolean isPrimary = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "booking_id",
            nullable = false
    )
    private Booking booking;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "trip_seat_id",
            nullable = false
    )
    private TripSeat tripSeat;

    @PrePersist
    protected void generateId() {
        if (this.bookingSeatId == null) {
            this.bookingSeatId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_BOOKING_SEAT);
        }
    }

}
