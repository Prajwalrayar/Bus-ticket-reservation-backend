package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, String> {

    Optional<Route> findBySourceIgnoreCaseAndDestinationIgnoreCase(
            String source,
            String destination
    );

    boolean existsBySourceIgnoreCaseAndDestinationIgnoreCase(
            String source,
            String destination
    );

    List<Route> findBySourceIgnoreCase(String source);

    List<Route> findByDestinationIgnoreCase(String destination);
}
