package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.TripSeatDTO;

import java.util.List;

public interface SeatService {

    List<TripSeatDTO> getTripSeats(String tripId);

    List<TripSeatDTO> lockSeats(String tripId, List<String> tripSeatIds, String userId);

    void releaseSeats(String tripId, List<String> tripSeatIds, String userId);

    void releaseExpiredLocks();
}
