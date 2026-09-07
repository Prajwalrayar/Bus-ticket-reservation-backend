package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.TripStopFare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripStopFareRepository extends JpaRepository<TripStopFare, String> {

    List<TripStopFare> findByTrip_TripId(String tripId);
}
