package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.RouteFareCreateRequest;
import com.crimsonlogic.busticketbooking.dto.RouteFareDTO;
import com.crimsonlogic.busticketbooking.entity.FareLocation;
import com.crimsonlogic.busticketbooking.entity.Route;
import com.crimsonlogic.busticketbooking.entity.RouteFare;
import com.crimsonlogic.busticketbooking.repository.FareLocationRepository;
import com.crimsonlogic.busticketbooking.repository.RouteFareRepository;
import com.crimsonlogic.busticketbooking.repository.RouteRepository;
import com.crimsonlogic.busticketbooking.service.RouteFareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteFareServiceImpl implements RouteFareService {

    private final RouteFareRepository routeFareRepository;
    private final FareLocationRepository fareLocationRepository;
    private final RouteRepository routeRepository;

    @Override
    public RouteFareDTO createRouteFare(
            String source,
            String destination,
            RouteFareCreateRequest request) {

        Route route = routeRepository
                .findBySourceIgnoreCaseAndDestinationIgnoreCase(source, destination)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Route '" + source + " → " + destination + "' not found"));

        FareLocation fromFL = fareLocationRepository.findById(request.getFromFareLocationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "FareLocation not found: " + request.getFromFareLocationId()));

        FareLocation toFL = fareLocationRepository.findById(request.getToFareLocationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "FareLocation not found: " + request.getToFareLocationId()));

        // Both FareLocations must belong to the same route
        if (!fromFL.getRoute().getRouteId().equals(route.getRouteId())) {
            throw new IllegalArgumentException(
                    "From-FareLocation does not belong to route '" + source + " → " + destination + "'");
        }
        if (!toFL.getRoute().getRouteId().equals(route.getRouteId())) {
            throw new IllegalArgumentException(
                    "To-FareLocation does not belong to route '" + source + " → " + destination + "'");
        }

        if (fromFL.getFareLocationId().equals(toFL.getFareLocationId())) {
            throw new IllegalArgumentException(
                    "From and To FareLocations must be different");
        }

        if (routeFareRepository.existsByRoute_RouteIdAndFromFareLocation_FareLocationIdAndToFareLocation_FareLocationId(
                route.getRouteId(), fromFL.getFareLocationId(), toFL.getFareLocationId())) {
            throw new IllegalArgumentException(
                    "A RouteFare already exists for this FareLocation pair");
        }

        RouteFare routeFare = new RouteFare();
        routeFare.setRoute(route);
        routeFare.setFromFareLocation(fromFL);
        routeFare.setToFareLocation(toFL);
        routeFare.setFare(request.getFare());

        RouteFare saved = routeFareRepository.save(routeFare);
        return convertToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteFareDTO> getRouteFares(String source, String destination) {
        return routeFareRepository
                .findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCase(source, destination)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public void deleteRouteFare(String routeFareId) {
        RouteFare routeFare = routeFareRepository.findById(routeFareId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "RouteFare not found: " + routeFareId));
        routeFareRepository.delete(routeFare);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private RouteFareDTO convertToDTO(RouteFare rf) {
        RouteFareDTO dto = new RouteFareDTO();
        dto.setRouteFareId(rf.getRouteFareId());
        dto.setFare(rf.getFare());
        if (rf.getRoute() != null) {
            dto.setRouteId(rf.getRoute().getRouteId());
        }
        if (rf.getFromFareLocation() != null) {
            dto.setFromFareLocationId(rf.getFromFareLocation().getFareLocationId());
            dto.setFromFareLocationName(rf.getFromFareLocation().getName());
        }
        if (rf.getToFareLocation() != null) {
            dto.setToFareLocationId(rf.getToFareLocation().getFareLocationId());
            dto.setToFareLocationName(rf.getToFareLocation().getName());
        }
        return dto;
    }
}
