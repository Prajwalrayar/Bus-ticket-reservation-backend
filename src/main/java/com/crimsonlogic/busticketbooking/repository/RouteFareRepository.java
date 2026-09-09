package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.RouteFare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteFareRepository extends JpaRepository<RouteFare, String> {

    List<RouteFare> findByRoute_RouteId(String routeId);

    List<RouteFare> findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCase(
            String source, String destination);

    Optional<RouteFare> findByFromFareLocation_FareLocationIdAndToFareLocation_FareLocationId(
            String fromFareLocationId, String toFareLocationId);

    boolean existsByRoute_RouteIdAndFromFareLocation_FareLocationIdAndToFareLocation_FareLocationId(
            String routeId, String fromFareLocationId, String toFareLocationId);
}
