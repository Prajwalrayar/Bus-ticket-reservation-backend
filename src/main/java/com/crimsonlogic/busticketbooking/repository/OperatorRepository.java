package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, String> {

    Optional<Operator> findByCompanyNameIgnoreCase(String companyName);

    Optional<Operator> findByContactEmailIgnoreCase(String contactEmail);

    boolean existsByCompanyNameIgnoreCase(String companyName);

    boolean existsByContactEmailIgnoreCase(String contactEmail);
}
