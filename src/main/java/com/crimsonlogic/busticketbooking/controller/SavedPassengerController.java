package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.SavedPassengerDTO;
import com.crimsonlogic.busticketbooking.dto.SavedPassengerRequest;
import com.crimsonlogic.busticketbooking.service.SavedPassengerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/saved-passengers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SavedPassengerController {

    private final SavedPassengerService savedPassengerService;


    // =========================================================
    // GET MY SAVED PASSENGERS
    // =========================================================

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedPassengerDTO>>>
    getMySavedPassengers() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        savedPassengerService
                                .getMySavedPassengers()
                )
        );
    }


    // =========================================================
    // GET ONE SAVED PASSENGER
    // =========================================================

    @GetMapping("/{savedPassengerId}")
    public ResponseEntity<ApiResponse<SavedPassengerDTO>>
    getMySavedPassenger(
            @PathVariable String savedPassengerId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        savedPassengerService
                                .getMySavedPassenger(
                                        savedPassengerId
                                )
                )
        );
    }


    // =========================================================
    // CREATE SAVED PASSENGER
    // =========================================================

    @PostMapping
    public ResponseEntity<ApiResponse<SavedPassengerDTO>>
    createSavedPassenger(
            @Valid @RequestBody SavedPassengerRequest request) {

        SavedPassengerDTO savedPassenger =
                savedPassengerService
                        .createSavedPassenger(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Passenger saved successfully",
                                savedPassenger
                        )
                );
    }


    // =========================================================
    // UPDATE SAVED PASSENGER
    // =========================================================

    @PutMapping("/{savedPassengerId}")
    public ResponseEntity<ApiResponse<SavedPassengerDTO>>
    updateSavedPassenger(
            @PathVariable String savedPassengerId,
            @Valid @RequestBody SavedPassengerRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Saved passenger updated successfully",
                        savedPassengerService
                                .updateSavedPassenger(
                                        savedPassengerId,
                                        request
                                )
                )
        );
    }


    // =========================================================
    // DEACTIVATE SAVED PASSENGER
    // =========================================================

    @DeleteMapping("/{savedPassengerId}")
    public ResponseEntity<ApiResponse<Void>>
    deactivateSavedPassenger(
            @PathVariable String savedPassengerId) {

        savedPassengerService
                .deactivateSavedPassenger(
                        savedPassengerId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Saved passenger removed successfully",
                        null
                )
        );
    }
}