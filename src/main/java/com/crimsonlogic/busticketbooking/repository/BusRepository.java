package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
