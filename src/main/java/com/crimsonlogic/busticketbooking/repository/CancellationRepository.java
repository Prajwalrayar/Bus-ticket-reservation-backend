package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Cancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CancellationRepository extends JpaRepository<Cancellation, String> {

    Optional<Cancellation> findByCancellationReferenceIgnoreCase(
            String cancellationReference
    );

    Optional<Cancellation> findByBooking_BookingId(
            String bookingId
    );

    boolean existsByBooking_BookingId(
            String bookingId
    );
}
