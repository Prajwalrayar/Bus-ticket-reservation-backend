package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, String> {

    List<Trip> findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndTravelDate(
            String source,
            String destination,
            LocalDate travelDate
    );

    List<Trip> findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCase(
            String source,
            String destination
    );

    List<Trip> findByRoute_RouteIdAndTravelDate(
            String routeId,
            LocalDate travelDate
    );

    List<Trip> findByBus_BusIdAndTravelDate(
            String busId,
            LocalDate travelDate
    );

    List<Trip> findByBus_Operator_OperatorId(
            String operatorId
    );

    List<Trip> findByRoute_SourceIgnoreCaseAndTravelDate(
            String source, 
            LocalDate travelDate
    );

    List<Trip> findByRoute_DestinationIgnoreCaseAndTravelDateBetween(
            String destination, 
            LocalDate startDate, 
            LocalDate endDate
    );

}
