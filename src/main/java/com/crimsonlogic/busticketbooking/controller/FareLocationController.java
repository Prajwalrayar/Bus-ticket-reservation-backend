package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.FareLocationCreateRequest;
import com.crimsonlogic.busticketbooking.dto.FareLocationDTO;
import com.crimsonlogic.busticketbooking.service.FareLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes/{source}/{destination}/fare-locations")
@RequiredArgsConstructor
public class FareLocationController {

    private final FareLocationService fareLocationService;

    /**
     * GET /api/routes/{source}/{destination}/fare-locations
     * Returns all fare zones defined for this route.
     * Publicly accessible (needed by booking UI).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FareLocationDTO>>> getFareLocations(
            @PathVariable String source,
            @PathVariable String destination) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        fareLocationService.getFareLocations(source, destination)
                )
        );
    }

    /**
     * POST /api/routes/{source}/{destination}/fare-locations
     * Create a new FareLocation on the route. ADMIN only.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FareLocationDTO>> createFareLocation(
            @PathVariable String source,
            @PathVariable String destination,
            @Valid @RequestBody FareLocationCreateRequest request) {

        FareLocationDTO created = fareLocationService.createFareLocation(source, destination, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fare location created successfully", created));
    }

    /**
     * DELETE /api/routes/{source}/{destination}/fare-locations/{fareLocationId}
     * Delete a FareLocation. ADMIN only.
     * Will fail if any stop is still assigned to it.
     */
    @DeleteMapping("/{fareLocationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFareLocation(
            @PathVariable String source,
            @PathVariable String destination,
            @PathVariable String fareLocationId) {

        fareLocationService.deleteFareLocation(fareLocationId);

        return ResponseEntity.ok(
                ApiResponse.success("Fare location deleted successfully", null)
        );
    }
}
