package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO;
import com.crimsonlogic.busticketbooking.dto.AdminLocationDTO;
import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.LocationAliasCreateRequest;
import com.crimsonlogic.busticketbooking.dto.LocationCreateRequest;
import com.crimsonlogic.busticketbooking.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/locations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLocationController {

    private final LocationService locationService;

    // --- Locations ---

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminLocationDTO>>> getAllLocations() {
        return ResponseEntity.ok(ApiResponse.success(locationService.getAllAdminLocations()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminLocationDTO>> createLocation(@Valid @RequestBody LocationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Location created successfully", locationService.createLocation(request)));
    }

    @PutMapping("/{locationId}")
    public ResponseEntity<ApiResponse<AdminLocationDTO>> updateLocationName(
            @PathVariable Long locationId,
            @Valid @RequestBody LocationCreateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Location updated successfully", locationService.updateLocationName(locationId, request)));
    }

    @PatchMapping("/{locationId}/status")
    public ResponseEntity<ApiResponse<AdminLocationDTO>> updateLocationStatus(
            @PathVariable Long locationId,
            @RequestBody Map<String, Boolean> statusMap) {
        boolean isActive = statusMap.getOrDefault("isActive", true);
        return ResponseEntity.ok(
                ApiResponse.success("Location status updated successfully", locationService.updateLocationStatus(locationId, isActive)));
    }

    // --- Aliases ---

    @GetMapping("/{locationId}/aliases")
    public ResponseEntity<ApiResponse<List<AdminLocationAliasDTO>>> getAliases(@PathVariable Long locationId) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getAliasesForLocation(locationId)));
    }

    @PostMapping("/{locationId}/aliases")
    public ResponseEntity<ApiResponse<AdminLocationAliasDTO>> createAlias(
            @PathVariable Long locationId,
            @Valid @RequestBody LocationAliasCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Alias created successfully", locationService.createAlias(locationId, request)));
    }

    @PutMapping("/{locationId}/aliases/{aliasId}")
    public ResponseEntity<ApiResponse<AdminLocationAliasDTO>> updateAliasName(
            @PathVariable Long locationId, // kept for route consistency if needed
            @PathVariable Long aliasId,
            @Valid @RequestBody LocationAliasCreateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Alias updated successfully", locationService.updateAliasName(aliasId, request)));
    }

    @PatchMapping("/{locationId}/aliases/{aliasId}/status")
    public ResponseEntity<ApiResponse<AdminLocationAliasDTO>> updateAliasStatus(
            @PathVariable Long locationId, // kept for route consistency
            @PathVariable Long aliasId,
            @RequestBody Map<String, Boolean> statusMap) {
        boolean isActive = statusMap.getOrDefault("isActive", true);
        return ResponseEntity.ok(
                ApiResponse.success("Alias status updated successfully", locationService.updateAliasStatus(aliasId, isActive)));
    }
}
