package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.FareLocationCreateRequest;
import com.crimsonlogic.busticketbooking.dto.FareLocationDTO;
import com.crimsonlogic.busticketbooking.entity.FareLocation;
import com.crimsonlogic.busticketbooking.entity.Route;
import com.crimsonlogic.busticketbooking.repository.FareLocationRepository;
import com.crimsonlogic.busticketbooking.repository.RouteRepository;
import com.crimsonlogic.busticketbooking.service.FareLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FareLocationServiceImpl implements FareLocationService {

    private final FareLocationRepository fareLocationRepository;
    private final RouteRepository routeRepository;

    @Override
    public FareLocationDTO createFareLocation(
            String source,
            String destination,
            FareLocationCreateRequest request) {

        Route route = routeRepository
                .findBySourceIgnoreCaseAndDestinationIgnoreCase(source, destination)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Route '" + source + " → " + destination + "' not found"));

        if (!Boolean.TRUE.equals(route.getIsActive())) {
            throw new IllegalArgumentException("Cannot add a FareLocation to an inactive route");
        }

        if (fareLocationRepository.existsByRoute_RouteIdAndNameIgnoreCase(
                route.getRouteId(), request.getName())) {
            throw new IllegalArgumentException(
                    "FareLocation '" + request.getName() + "' already exists on this route");
        }

        FareLocation fareLocation = new FareLocation();
        fareLocation.setName(request.getName());
        fareLocation.setDescription(request.getDescription());
        fareLocation.setRoute(route);

        FareLocation saved = fareLocationRepository.save(fareLocation);
        return convertToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FareLocationDTO> getFareLocations(String source, String destination) {
        return fareLocationRepository
                .findByRoute_SourceIgnoreCaseAndRoute_DestinationIgnoreCase(source, destination)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public void deleteFareLocation(String fareLocationId) {
        FareLocation fareLocation = fareLocationRepository.findById(fareLocationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "FareLocation not found: " + fareLocationId));

        // Guard: don't allow deletion if stops are still assigned
        if (!fareLocation.getRouteStops().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot delete FareLocation '" + fareLocation.getName()
                            + "' — " + fareLocation.getRouteStops().size()
                            + " stop(s) are still assigned to it. "
                            + "Reassign or clear those stops first.");
        }

        fareLocationRepository.delete(fareLocation);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private FareLocationDTO convertToDTO(FareLocation fl) {
        FareLocationDTO dto = new FareLocationDTO();
        dto.setFareLocationId(fl.getFareLocationId());
        dto.setName(fl.getName());
        dto.setDescription(fl.getDescription());
        if (fl.getRoute() != null) {
            dto.setRouteId(fl.getRoute().getRouteId());
            dto.setSource(fl.getRoute().getSource());
            dto.setDestination(fl.getRoute().getDestination());
        }
        return dto;
    }
}
