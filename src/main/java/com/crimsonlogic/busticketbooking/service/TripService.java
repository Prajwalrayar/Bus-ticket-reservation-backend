package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.SeatLayoutDTO;
import com.crimsonlogic.busticketbooking.dto.TripCreateRequest;
import com.crimsonlogic.busticketbooking.dto.TripDTO;
import com.crimsonlogic.busticketbooking.dto.TripSearchRequest;
import com.crimsonlogic.busticketbooking.dto.TripSeatDTO;

import java.time.LocalDate;
import java.util.List;

public interface TripService {

    TripDTO createTrip(TripCreateRequest request);

    TripDTO updateTrip(
            String busRegistrationNumber,
            String source,
            String destination,
            LocalDate travelDate,
            TripCreateRequest request
    );

    List<TripDTO> searchTrips(
            TripSearchRequest request
    );

    TripDTO getTripById(String tripId);

    List<TripDTO> getAllTrips();

    /** Returns trips for the current user's operator company (for BUS_OPERATOR / SUPPORT_AGENT) */
    List<TripDTO> getOperatorTrips();

    void cancelTrip(
            String busRegistrationNumber,
            String source,
            String destination,
            LocalDate travelDate,
            String reason
    );

    List<TripSeatDTO> getTripSeats(
            String busRegistrationNumber,
            String source,
            String destination,
            LocalDate travelDate
    );

    List<TripSeatDTO> getAvailableSeats(
            String busRegistrationNumber,
            String source,
            String destination,
            LocalDate travelDate
    );

    SeatLayoutDTO getSeatLayout(String tripId);

    /** Back-fills TripSeat rows for any trip that currently has none. */
    int backfillTripSeats();

    /**
     * Get passenger analytics for a trip.
     */
    List<com.crimsonlogic.busticketbooking.dto.PassengerAnalyticsDTO> getTripPassengers(String tripId);
}