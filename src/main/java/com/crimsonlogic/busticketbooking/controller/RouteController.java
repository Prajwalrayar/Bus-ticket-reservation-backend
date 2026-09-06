package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.RouteCreateRequest;
import com.crimsonlogic.busticketbooking.dto.RouteDTO;
import com.crimsonlogic.busticketbooking.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    /*
     * Get all routes.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RouteDTO>>> getAllRoutes() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        routeService.getAllRoutes()
                )
        );
    }

    /*
     * Find a specific route using its
     * business-facing source and destination.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<RouteDTO>> getRoute(
            @RequestParam String source,
            @RequestParam String destination) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        routeService.getRoute(
                                source,
                                destination
                        )
                )
        );
    }

    /*
     * Find routes starting from a particular source.
     */
    @GetMapping("/source/{source}")
    public ResponseEntity<ApiResponse<List<RouteDTO>>> getRoutesBySource(
            @PathVariable String source) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        routeService.getRoutesBySource(source)
                )
        );
    }

    /*
     * Find routes ending at a particular destination.
     */
    @GetMapping("/destination/{destination}")
    public ResponseEntity<ApiResponse<List<RouteDTO>>> getRoutesByDestination(
            @PathVariable String destination) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        routeService.getRoutesByDestination(destination)
                )
        );
    }

    /*
     * Routes are master data managed by the administrator.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RouteDTO>> createRoute(
            @Valid @RequestBody RouteCreateRequest request) {

        RouteDTO createdRoute =
                routeService.createRoute(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Route created successfully",
                                createdRoute
                        )
                );
    }

    /*
     * Update an existing route using its
     * current source and destination.
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RouteDTO>> updateRoute(
            @RequestParam String source,
            @RequestParam String destination,
            @Valid @RequestBody RouteCreateRequest request) {

        RouteDTO updatedRoute =
                routeService.updateRoute(
                        source,
                        destination,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Route updated successfully",
                        updatedRoute
                )
        );
    }

    /*
     * Soft-deactivate a route.
     *
     * The route is not physically deleted because
     * existing trips may still reference it.
     */
    @PatchMapping("/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateRoute(
            @RequestParam String source,
            @RequestParam String destination) {

        routeService.deactivateRoute(
                source,
                destination
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Route deactivated successfully",
                        null
                )
        );
    }
}