package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.RouteStopCreateRequest;
import com.crimsonlogic.busticketbooking.dto.RouteStopDTO;
import com.crimsonlogic.busticketbooking.entity.FareLocation;
import com.crimsonlogic.busticketbooking.entity.Route;
import com.crimsonlogic.busticketbooking.entity.RouteStop;
import com.crimsonlogic.busticketbooking.enums.StopType;
import com.crimsonlogic.busticketbooking.repository.FareLocationRepository;
import com.crimsonlogic.busticketbooking.repository.RouteRepository;
import com.crimsonlogic.busticketbooking.repository.RouteStopRepository;
import com.crimsonlogic.busticketbooking.service.RouteStopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteStopServiceImpl implements RouteStopService {

    private final RouteStopRepository routeStopRepository;
    private final RouteRepository routeRepository;
    private final FareLocationRepository fareLocationRepository;

    @Override
    public RouteStopDTO createRouteStop(
            String source,
            String destination,
            RouteStopCreateRequest request) {

        Route route = findRoute(source, destination);

        if (!Boolean.TRUE.equals(route.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot add a stop to an inactive route"
            );
        }

        /*
         * Stop sequence must be unique within a route per StopType.
         * If the new stop is BOTH, it shouldn't conflict with any existing sequence.
         */
        boolean sequenceExists = false;
        if (request.getStopType() == StopType.INTERMEDIATE) {
             sequenceExists = routeStopRepository
                .existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopSequenceAndStopType(
                        source, destination, request.getStopSequence(), StopType.BOARDING) ||
                routeStopRepository
                .existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopSequenceAndStopType(
                        source, destination, request.getStopSequence(), StopType.DROPPING) ||
                routeStopRepository
                .existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopSequenceAndStopType(
                        source, destination, request.getStopSequence(), StopType.INTERMEDIATE);
        } else {
             sequenceExists = routeStopRepository
                .existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopSequenceAndStopType(
                        source, destination, request.getStopSequence(), request.getStopType()) ||
                routeStopRepository
                .existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopSequenceAndStopType(
                        source, destination, request.getStopSequence(), StopType.INTERMEDIATE);
        }

        if (sequenceExists) {
            throw new IllegalArgumentException(
                    "Stop sequence "
                            + request.getStopSequence()
                            + " already exists for " + request.getStopType() + " on this route"
            );
        }

        /*
         * Stop name should not be duplicated within
         * the same route.
         */
        if (routeStopRepository
                .existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopNameIgnoreCase(
                        source,
                        destination,
                        request.getStopName()
                )) {

            throw new IllegalArgumentException(
                    "Stop '"
                            + request.getStopName()
                            + "' already exists on this route"
            );
        }

        /*
         * The source stop must be at the beginning
         * and destination stop at the end.
         */
        validateStopTypeAndSequence(
                route,
                request
        );

        RouteStop routeStop = new RouteStop();

        routeStop.setStopName(
                request.getStopName()
        );

        routeStop.setStopSequence(
                request.getStopSequence()
        );

        routeStop.setStopType(
                request.getStopType()
        );

        routeStop.setDistanceFromSourceKm(
                request.getDistanceFromSourceKm()
        );

        // Optional: assign to a FareLocation
        if (request.getFareLocationId() != null && !request.getFareLocationId().isBlank()) {
            FareLocation fareLocation = fareLocationRepository.findById(request.getFareLocationId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "FareLocation not found: " + request.getFareLocationId()));
            routeStop.setFareLocation(fareLocation);
        }

        // Boarding / dropping flags
        routeStop.setCanBoard(request.getCanBoard() != null ? request.getCanBoard() : true);
        routeStop.setCanDrop(request.getCanDrop() != null ? request.getCanDrop() : true);

        routeStop.setRoute(route);

        RouteStop savedStop =
                routeStopRepository.save(routeStop);

        return convertToDTO(savedStop);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteStopDTO getRouteStop(
            String source,
            String destination,
            String stopName) {

        RouteStop routeStop =
                routeStopRepository
                        .findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopNameIgnoreCase(
                                source,
                                destination,
                                stopName
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Stop '"
                                                + stopName
                                                + "' not found on route '"
                                                + source
                                                + " → "
                                                + destination
                                                + "'"
                                )
                        );

        return convertToDTO(routeStop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteStopDTO> getRouteStops(
            String source,
            String destination) {

        /*
         * Verify that the route exists.
         */
        findRoute(source, destination);

        return routeStopRepository
                .findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseOrderByStopSequenceAsc(
                        source,
                        destination
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteStopDTO> getRouteStopsByType(
            String source,
            String destination,
            StopType stopType) {

        findRoute(source, destination);

        return routeStopRepository
                .findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopType(
                        source,
                        destination,
                        stopType
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public RouteStopDTO updateRouteStop(
            String source,
            String destination,
            String stopName,
            RouteStopCreateRequest request) {

        RouteStop existingStop =
                routeStopRepository
                        .findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopNameIgnoreCase(
                                source,
                                destination,
                                stopName
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Stop '"
                                                + stopName
                                                + "' not found on route '"
                                                + source
                                                + " → "
                                                + destination
                                                + "'"
                                )
                        );

        /*
         * Check whether the new stop sequence is already
         * used by another stop on the route for this type.
         */
        if (!existingStop.getStopSequence().equals(request.getStopSequence()) || existingStop.getStopType() != request.getStopType()) {
            boolean sequenceExists = false;
            if (request.getStopType() == StopType.INTERMEDIATE) {
                 sequenceExists = routeStopRepository
                    .existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopSequenceAndStopType(
                            source, destination, request.getStopSequence(), StopType.BOARDING) ||
                    routeStopRepository
                    .existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopSequenceAndStopType(
                            source, destination, request.getStopSequence(), StopType.DROPPING) ||
                    routeStopRepository
                    .existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopSequenceAndStopType(
                            source, destination, request.getStopSequence(), StopType.INTERMEDIATE);
            } else {
                 sequenceExists = routeStopRepository
                    .existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopSequenceAndStopType(
                            source, destination, request.getStopSequence(), request.getStopType()) ||
                    routeStopRepository
                    .existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopSequenceAndStopType(
                            source, destination, request.getStopSequence(), StopType.INTERMEDIATE);
            }
            
            if (sequenceExists) {
                throw new IllegalArgumentException(
                        "Stop sequence "
                                + request.getStopSequence()
                                + " already exists for " + request.getStopType() + " on this route"
                );
            }
        }

        /*
         * If the stop name is changed, make sure the new
         * name doesn't already exist on the route.
         */
        if (!existingStop.getStopName()
                .equalsIgnoreCase(request.getStopName())
                && routeStopRepository
                .existsByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseAndStopNameIgnoreCase(
                        source,
                        destination,
                        request.getStopName()
                )) {

            throw new IllegalArgumentException(
                    "Stop '"
                            + request.getStopName()
                            + "' already exists on this route"
            );
        }

        existingStop.setStopName(
                request.getStopName()
        );

        existingStop.setStopSequence(
                request.getStopSequence()
        );

        existingStop.setStopType(
                request.getStopType()
        );

        existingStop.setDistanceFromSourceKm(
                request.getDistanceFromSourceKm()
        );

        // Update FareLocation assignment
        if (request.getFareLocationId() != null && !request.getFareLocationId().isBlank()) {
            FareLocation fareLocation = fareLocationRepository.findById(request.getFareLocationId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "FareLocation not found: " + request.getFareLocationId()));
            existingStop.setFareLocation(fareLocation);
        } else {
            // Explicitly clearing the fare location assignment
            existingStop.setFareLocation(null);
        }

        // Update boarding / dropping flags
        if (request.getCanBoard() != null) {
            existingStop.setCanBoard(request.getCanBoard());
        }
        if (request.getCanDrop() != null) {
            existingStop.setCanDrop(request.getCanDrop());
        }

        RouteStop updatedStop =
                routeStopRepository.save(existingStop);

        return convertToDTO(updatedStop);
    }

    /*
     * Find route using its business identity:
     * source + destination.
     */
    private Route findRoute(
            String source,
            String destination) {

        return routeRepository
                .findBySourceIgnoreCaseAndDestinationIgnoreCase(
                        source,
                        destination
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Route from '"
                                        + source
                                        + "' to '"
                                        + destination
                                        + "' not found"
                        )
                );
    }

    /*
     * Validates logical position of source/destination stops.
     */
    private void validateStopTypeAndSequence(
            Route route,
            RouteStopCreateRequest request) {

        if (request.getStopSequence() <= 0) {
            throw new IllegalArgumentException(
                    "Stop sequence must be greater than zero"
            );
        }

        if (request.getDistanceFromSourceKm() != null
                && request.getDistanceFromSourceKm().compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new IllegalArgumentException(
                    "Distance from source cannot be negative"
            );
        }

        List<RouteStop> existingStops =
                routeStopRepository
                        .findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCaseOrderByStopSequenceAsc(
                                route.getSource(),
                                route.getDestination()
                        );

        /*
         * A boarding point should occur before any
         * dropping point on the route.
         */
        if (request.getStopType() == StopType.BOARDING) {

            boolean droppingPointAfterThisStop =
                    existingStops.stream()
                            .anyMatch(stop ->
                                    stop.getStopType() == StopType.DROPPING
                                            && stop.getDistanceFromSourceKm().compareTo(request.getDistanceFromSourceKm()) < 0
                            );

            if (droppingPointAfterThisStop) {
                throw new IllegalArgumentException(
                        "A boarding point cannot occur after a dropping point"
                );
            }
        }

        /*
         * A dropping point should occur after any
         * boarding point on the route.
         */
        if (request.getStopType() == StopType.DROPPING) {

            boolean boardingPointAfterThisStop =
                    existingStops.stream()
                            .anyMatch(stop ->
                                    stop.getStopType() == StopType.BOARDING
                                            && stop.getDistanceFromSourceKm().compareTo(request.getDistanceFromSourceKm()) > 0
                            );

            if (boardingPointAfterThisStop) {
                throw new IllegalArgumentException(
                        "A dropping point cannot occur before a boarding point"
                );
            }
        }
    }

    private RouteStopDTO convertToDTO(
            RouteStop routeStop) {

        RouteStopDTO dto = new RouteStopDTO();

        dto.setRouteStopId(
                routeStop.getRouteStopId()
        );

        dto.setStopName(
                routeStop.getStopName()
        );

        dto.setStopSequence(
                routeStop.getStopSequence()
        );

        dto.setStopType(
                routeStop.getStopType()
        );

        dto.setDistanceFromSourceKm(
                routeStop.getDistanceFromSourceKm()
        );

        if (routeStop.getRoute() != null) {
            dto.setSource(
                    routeStop.getRoute().getSource()
            );

            dto.setDestination(
                    routeStop.getRoute().getDestination()
            );
        }

        // FareLocation
        if (routeStop.getFareLocation() != null) {
            dto.setFareLocationId(routeStop.getFareLocation().getFareLocationId());
            dto.setFareLocationName(routeStop.getFareLocation().getName());
        }

        // Boarding / dropping flags
        dto.setCanBoard(routeStop.getCanBoard());
        dto.setCanDrop(routeStop.getCanDrop());

        return dto;
    }
}