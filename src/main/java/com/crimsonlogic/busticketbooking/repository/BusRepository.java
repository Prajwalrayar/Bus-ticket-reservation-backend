package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Bus;
import com.crimsonlogic.busticketbooking.enums.BusActivationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BusRepository extends JpaRepository<Bus, String> {

    Optional<Bus> findByRegistrationNumberIgnoreCase(
            String registrationNumber
    );

    boolean existsByRegistrationNumberIgnoreCase(
            String registrationNumber
    );

    List<Bus> findByOperator_CompanyNameIgnoreCase(
            String companyName
    );

    long countByIsActiveTrue();

    long countByOperator(com.crimsonlogic.busticketbooking.entity.Operator operator);

    /** Buses with a pending activation request. */
    List<Bus> findByActivationRequestStatus(BusActivationStatus status);

    /**
     * Find all active buses that have had NO trip at all since the given cutoff date.
     * Used by the auto-deactivation scheduler.
     */
    @Query("SELECT b FROM Bus b WHERE b.isActive = true " +
           "AND (b.lastTripDate IS NULL OR b.lastTripDate < :cutoff)")
    List<Bus> findActiveBusesInactiveSince(@Param("cutoff") LocalDate cutoff);
}
