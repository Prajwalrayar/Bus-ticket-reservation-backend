package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.FareLocationCreateRequest;
import com.crimsonlogic.busticketbooking.dto.FareLocationDTO;

import java.util.List;

public interface FareLocationService {

    /**
     * Create a new FareLocation on the given route.
     */
    FareLocationDTO createFareLocation(
            String source,
            String destination,
            FareLocationCreateRequest request
    );

    /**
     * Get all FareLocations for a route.
     */
    List<FareLocationDTO> getFareLocations(
            String source,
            String destination
    );

    /**
     * Delete a FareLocation by ID.
     * Will fail if any RouteStop is still assigned to this FareLocation.
     */
    void deleteFareLocation(String fareLocationId);
}
