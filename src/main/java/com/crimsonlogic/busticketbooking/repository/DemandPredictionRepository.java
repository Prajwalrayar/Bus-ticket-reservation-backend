package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.DemandPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DemandPredictionRepository extends JpaRepository<DemandPrediction, String> {
    Optional<DemandPrediction> findByTrip_TripId(String tripId);
}
