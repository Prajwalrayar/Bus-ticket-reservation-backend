package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.FareLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FareLocationRepository extends JpaRepository<FareLocation, String> {

    List<FareLocation> findByRoute_RouteId(String routeId);

    List<FareLocation> findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCase(
            String source, String destination);

    boolean existsByRoute_RouteIdAndNameIgnoreCase(String routeId, String name);
}
