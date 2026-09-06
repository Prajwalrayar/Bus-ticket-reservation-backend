package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.RouteStop;
import com.crimsonlogic.busticketbooking.enums.StopType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, String> {

    List<RouteStop>
    findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseOrderByStopSequenceAsc(
            String source,
            String destination
    );

    Optional<RouteStop>
    findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopNameIgnoreCase(
            String source,
            String destination,
            String stopName
    );

    boolean
    existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopSequenceAndStopType(
            String source,
            String destination,
            Integer stopSequence,
            StopType stopType
    );

    boolean
    existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopNameIgnoreCase(
            String source,
            String destination,
            String stopName
    );

    List<RouteStop>
    findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopType(
            String source,
            String destination,
            StopType stopType
    );
}
