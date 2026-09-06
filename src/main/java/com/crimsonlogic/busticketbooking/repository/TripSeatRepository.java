package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.TripSeat;
import com.crimsonlogic.busticketbooking.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripSeatRepository extends JpaRepository<TripSeat, String> {

    List<TripSeat> findByTrip_TripIdOrderByBusSeat_SeatNumberAsc(
            String tripId
    );

    List<TripSeat> findByTrip_TripIdAndSeatStatus(
            String tripId,
            SeatStatus seatStatus
    );

    Optional<TripSeat> findByTrip_TripIdAndBusSeat_SeatNumber(
            String tripId,
            String seatNumber
    );

    List<TripSeat> findBySeatStatusAndLockExpiryTimeBefore(
            SeatStatus seatStatus,
            LocalDateTime expiryBefore
    );

    long countByTrip_TripId(String tripId);
}

