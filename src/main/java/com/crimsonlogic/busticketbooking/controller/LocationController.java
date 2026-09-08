package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.LocationDTO;
import com.crimsonlogic.busticketbooking.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<LocationDTO>>> getSuggestions(
            @RequestParam String query) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        locationService.getSuggestions(query)
                )
        );
    }
}
