package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.RouteFareCreateRequest;
import com.crimsonlogic.busticketbooking.dto.RouteFareDTO;

import java.util.List;

public interface RouteFareService {

    /**
     * Create a new RouteFare configuration between two FareLocations.
     */
    RouteFareDTO createRouteFare(
            String source,
            String destination,
            RouteFareCreateRequest request
    );

    /**
     * Get all RouteFares for a route.
     */
    List<RouteFareDTO> getRouteFares(
            String source,
            String destination
    );

    /**
     * Delete a RouteFare by ID.
     */
    void deleteRouteFare(String routeFareId);
}
