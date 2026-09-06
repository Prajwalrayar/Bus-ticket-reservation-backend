package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.RouteStopCreateRequest;
import com.crimsonlogic.busticketbooking.dto.RouteStopDTO;
import com.crimsonlogic.busticketbooking.enums.StopType;
import com.crimsonlogic.busticketbooking.service.RouteStopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes/{source}/{destination}/stops")
@RequiredArgsConstructor
public class RouteStopController {

    private final RouteStopService routeStopService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RouteStopDTO>>>
    getRouteStops(
            @PathVariable String source,
            @PathVariable String destination) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        routeStopService.getRouteStops(
                                source,
                                destination
                        )
                )
        );
    }

    @GetMapping("/{stopName}")
    public ResponseEntity<ApiResponse<RouteStopDTO>>
    getRouteStop(
            @PathVariable String source,
            @PathVariable String destination,
            @PathVariable String stopName) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        routeStopService.getRouteStop(
                                source,
                                destination,
                                stopName
                        )
                )
        );
    }

    @GetMapping("/type/{stopType}")
    public ResponseEntity<ApiResponse<List<RouteStopDTO>>>
    getRouteStopsByType(
            @PathVariable String source,
            @PathVariable String destination,
            @PathVariable StopType stopType) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        routeStopService.getRouteStopsByType(
                                source,
                                destination,
                                stopType
                        )
                )
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RouteStopDTO>>
    createRouteStop(
            @PathVariable String source,
            @PathVariable String destination,
            @Valid @RequestBody RouteStopCreateRequest request) {

        RouteStopDTO createdStop =
                routeStopService.createRouteStop(
                        source,
                        destination,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Route stop created successfully",
                                createdStop
                        )
                );
    }

    @PutMapping("/{stopName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RouteStopDTO>>
    updateRouteStop(
            @PathVariable String source,
            @PathVariable String destination,
            @PathVariable String stopName,
            @Valid @RequestBody RouteStopCreateRequest request) {

        RouteStopDTO updatedStop =
                routeStopService.updateRouteStop(
                        source,
                        destination,
                        stopName,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Route stop updated successfully",
                        updatedStop
                )
        );
    }
}