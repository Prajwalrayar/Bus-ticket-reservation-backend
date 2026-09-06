package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.BusSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusSeatRepository extends JpaRepository<BusSeat, String> {

    Optional<BusSeat> findByBus_RegistrationNumberIgnoreCaseAndSeatNumberIgnoreCase(
            String registrationNumber,
            String seatNumber
    );

    List<BusSeat> findByBus_RegistrationNumberIgnoreCase(
            String registrationNumber
    );

    boolean existsByBus_RegistrationNumberIgnoreCaseAndSeatNumberIgnoreCase(
            String registrationNumber,
            String seatNumber
    );
}
