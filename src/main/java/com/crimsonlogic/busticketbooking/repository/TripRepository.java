package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, String> {

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Trip t " +
            "LEFT JOIN t.route.routeStops rs1 " +
            "LEFT JOIN t.route.routeStops rs2 " +
            "WHERE (LOWER(t.route.source) = LOWER(:source) OR LOWER(rs1.stopName) = LOWER(:source)) " +
            "AND (LOWER(t.route.destination) = LOWER(:destination) OR LOWER(rs2.stopName) = LOWER(:destination)) " +
            "AND (rs1 IS NULL OR rs2 IS NULL OR rs1.stopSequence < rs2.stopSequence) " +
            "AND t.travelDate = :travelDate")
    List<Trip> findByIntermediateStopsAndTravelDate(
            @org.springframework.data.repository.query.Param("source") String source,
            @org.springframework.data.repository.query.Param("destination") String destination,
            @org.springframework.data.repository.query.Param("travelDate") LocalDate travelDate
    );

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t FROM Trip t " +
            "LEFT JOIN t.route.routeStops rs1 " +
            "LEFT JOIN t.route.routeStops rs2 " +
            "WHERE (LOWER(t.route.source) = LOWER(:source) OR LOWER(rs1.stopName) = LOWER(:source)) " +
            "AND (LOWER(t.route.destination) = LOWER(:destination) OR LOWER(rs2.stopName) = LOWER(:destination)) " +
            "AND (rs1 IS NULL OR rs2 IS NULL OR rs1.stopSequence < rs2.stopSequence)")
    List<Trip> findByIntermediateStops(
            @org.springframework.data.repository.query.Param("source") String source,
            @org.springframework.data.repository.query.Param("destination") String destination
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
