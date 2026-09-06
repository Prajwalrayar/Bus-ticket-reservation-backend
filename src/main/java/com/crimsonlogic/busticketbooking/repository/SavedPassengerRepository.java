package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.SavedPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPassengerRepository extends JpaRepository<SavedPassenger, String> {

    List<SavedPassenger> findByUser_UserIdAndIsActiveTrueOrderByPassengerNameAsc(
            String userId
    );

    Optional<SavedPassenger> findBySavedPassengerIdAndUser_UserId(
            String savedPassengerId,
            String userId
    );

    boolean existsByPassengerNameAndContactNumberAndUser_UserId(
            String passengerName,
            String contactNumber,
            String userId
    );
}
