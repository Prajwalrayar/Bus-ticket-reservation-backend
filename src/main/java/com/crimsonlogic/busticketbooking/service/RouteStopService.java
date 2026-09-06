package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.RouteStopCreateRequest;
import com.crimsonlogic.busticketbooking.dto.RouteStopDTO;
import com.crimsonlogic.busticketbooking.enums.StopType;

import java.util.List;

public interface RouteStopService {

    RouteStopDTO createRouteStop(
            String source,
            String destination,
            RouteStopCreateRequest request
    );

    RouteStopDTO getRouteStop(
            String source,
            String destination,
            String stopName
    );

    List<RouteStopDTO> getRouteStops(
            String source,
            String destination
    );

    List<RouteStopDTO> getRouteStopsByType(
            String source,
            String destination,
            StopType stopType
    );

    RouteStopDTO updateRouteStop(
            String source,
            String destination,
            String stopName,
            RouteStopCreateRequest request
    );
}