package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.TripSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripSegmentRepository extends JpaRepository<TripSegment, String> {
    List<TripSegment> findByTrip_TripId(String tripId);
}
