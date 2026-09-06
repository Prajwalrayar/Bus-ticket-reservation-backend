package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {

    Optional<Ticket> findByTicketNumberIgnoreCase(
            String ticketNumber
    );

    Optional<Ticket> findByBooking_BookingId(
            String bookingId
    );

    boolean existsByTicketNumberIgnoreCase(
            String ticketNumber
    );
}
