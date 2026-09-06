package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.SeatLayoutDTO;
import com.crimsonlogic.busticketbooking.dto.TripCreateRequest;
import com.crimsonlogic.busticketbooking.dto.TripDTO;
import com.crimsonlogic.busticketbooking.dto.TripSearchRequest;
import com.crimsonlogic.busticketbooking.dto.TripSeatDTO;
import com.crimsonlogic.busticketbooking.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TripDTO>>> searchTrips(
            @Valid @ModelAttribute TripSearchRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        tripService.searchTrips(request)
                )
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TripDTO>>> getAllTrips() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        tripService.getAllTrips()
                )
        );
    }

    @GetMapping("/operator")
    @PreAuthorize("hasAnyRole('BUS_OPERATOR', 'SUPPORT_AGENT')")
    public ResponseEntity<ApiResponse<List<TripDTO>>> getOperatorTrips() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        tripService.getOperatorTrips()
                )
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<TripDTO>> createTrip(
            @Valid @RequestBody TripCreateRequest request) {

        TripDTO trip =
                tripService.createTrip(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Trip created successfully",
                                trip
                        )
                );
    }

    @PutMapping
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<TripDTO>> updateTrip(
            @RequestParam String busRegistrationNumber,
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam LocalDate travelDate,
            @Valid @RequestBody TripCreateRequest request) {

        TripDTO trip =
                tripService.updateTrip(
                        busRegistrationNumber,
                        source,
                        destination,
                        travelDate,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Trip updated successfully",
                        trip
                )
        );
    }

    @PatchMapping("/cancel")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> cancelTrip(
            @RequestParam String busRegistrationNumber,
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam LocalDate travelDate,
            @RequestParam String reason) {

        tripService.cancelTrip(
                busRegistrationNumber,
                source,
                destination,
                travelDate,
                reason
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Trip cancelled successfully",
                        null
                )
        );
    }

    @GetMapping("/seats")
    public ResponseEntity<ApiResponse<List<TripSeatDTO>>> getTripSeats(
            @RequestParam String busRegistrationNumber,
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam LocalDate travelDate) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        tripService.getTripSeats(
                                busRegistrationNumber,
                                source,
                                destination,
                                travelDate
                        )
                )
        );
    }

    @GetMapping("/seats/available")
    public ResponseEntity<ApiResponse<List<TripSeatDTO>>>
    getAvailableSeats(
            @RequestParam String busRegistrationNumber,
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam LocalDate travelDate) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        tripService.getAvailableSeats(
                                busRegistrationNumber,
                                source,
                                destination,
                                travelDate
                        )
                )
        );
    }

    /**
     * Returns a 2D seat-layout grouped by LOWER / UPPER deck.
     * Used by the SeatLayoutComponent on the frontend.
     */
    @GetMapping("/{tripId}/seat-layout")
    public ResponseEntity<ApiResponse<SeatLayoutDTO>> getSeatLayout(
            @PathVariable String tripId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        tripService.getSeatLayout(tripId)
                )
        );
    }

    /**
     * Get a single trip by its ID.
     */
    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripDTO>> getTripById(
            @PathVariable String tripId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        tripService.getTripById(tripId)
                )
        );
    }

    /**
     * Admin-only endpoint to back-fill TripSeat rows for old trips that have none.
     */
    @PostMapping("/admin/backfill-seats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> backfillTripSeats() {
        int tripsFixed = tripService.backfillTripSeats();
        return ResponseEntity.ok(
                ApiResponse.success("Back-filled seats for " + tripsFixed + " trip(s).")
        );
    }
}