package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.RouteFareCreateRequest;
import com.crimsonlogic.busticketbooking.dto.RouteFareDTO;
import com.crimsonlogic.busticketbooking.service.RouteFareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes/{source}/{destination}/route-fares")
@RequiredArgsConstructor
public class RouteFareController {

    private final RouteFareService routeFareService;

    /**
     * GET /api/routes/{source}/{destination}/route-fares
     * Returns all FareLocation→FareLocation fares for this route.
     * Publicly accessible (needed by booking/search UI).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RouteFareDTO>>> getRouteFares(
            @PathVariable String source,
            @PathVariable String destination) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        routeFareService.getRouteFares(source, destination)
                )
        );
    }

    /**
     * POST /api/routes/{source}/{destination}/route-fares
     * Create a new RouteFare between two FareLocations. ADMIN only.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RouteFareDTO>> createRouteFare(
            @PathVariable String source,
            @PathVariable String destination,
            @Valid @RequestBody RouteFareCreateRequest request) {

        RouteFareDTO created = routeFareService.createRouteFare(source, destination, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Route fare created successfully", created));
    }

    /**
     * DELETE /api/routes/{source}/{destination}/route-fares/{routeFareId}
     * Delete a RouteFare. ADMIN only.
     */
    @DeleteMapping("/{routeFareId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRouteFare(
            @PathVariable String source,
            @PathVariable String destination,
            @PathVariable String routeFareId) {

        routeFareService.deleteRouteFare(routeFareId);

        return ResponseEntity.ok(
                ApiResponse.success("Route fare deleted successfully", null)
        );
    }
}
