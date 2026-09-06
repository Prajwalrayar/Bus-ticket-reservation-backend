package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.RouteCreateRequest;
import com.crimsonlogic.busticketbooking.dto.RouteDTO;

import java.util.List;

public interface RouteService {

    RouteDTO createRoute(RouteCreateRequest request);

    RouteDTO getRoute(
            String source,
            String destination
    );

    List<RouteDTO> getAllRoutes();

    List<RouteDTO> getRoutesBySource(String source);

    List<RouteDTO> getRoutesByDestination(String destination);

    RouteDTO updateRoute(
            String source,
            String destination,
            RouteCreateRequest request
    );

    void deactivateRoute(
            String source,
            String destination
    );
}
