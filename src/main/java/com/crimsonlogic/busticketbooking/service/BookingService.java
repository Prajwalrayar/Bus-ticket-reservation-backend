package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.BookingCancelRequest;
import com.crimsonlogic.busticketbooking.dto.BookingCreateRequest;
import com.crimsonlogic.busticketbooking.dto.BookingDTO;

import java.util.List;

public interface BookingService {

    BookingDTO createBooking(
            BookingCreateRequest request
    );

    BookingDTO getBookingById(
            String bookingId
    );

    BookingDTO getBookingByReference(
            String bookingReference
    );

    List<BookingDTO> getBookingsByUser(
            String userId
    );

    List<BookingDTO> getMyBookings();

    List<BookingDTO> getAllBookings();

    BookingDTO cancelBooking(
            String bookingId,
            BookingCancelRequest request
    );
}