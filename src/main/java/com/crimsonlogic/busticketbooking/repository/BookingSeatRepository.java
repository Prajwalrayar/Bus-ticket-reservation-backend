package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, String> {

    List<BookingSeat> findByBooking_BookingId(
            String bookingId
    );
}
