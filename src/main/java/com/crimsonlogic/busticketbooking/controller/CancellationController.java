package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.CancellationDTO;
import com.crimsonlogic.busticketbooking.dto.CancellationRequest;
import com.crimsonlogic.busticketbooking.service.CancellationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CancellationController {

    private final CancellationService cancellationService;


    // =========================================================
    // CANCEL BOOKING
    // =========================================================

    @PostMapping("/bookings/{bookingId}/cancellation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CancellationDTO>>
    cancelBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody CancellationRequest request) {

        CancellationDTO cancellation =
                cancellationService.cancelBooking(
                        bookingId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Booking cancelled successfully",
                                cancellation
                        )
                );
    }


    // =========================================================
    // GET CANCELLATION BY ID
    // =========================================================

    @GetMapping("/cancellations/{cancellationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CancellationDTO>>
    getCancellationById(
            @PathVariable String cancellationId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        cancellationService
                                .getCancellationById(
                                        cancellationId
                                )
                )
        );
    }


    // =========================================================
    // GET CANCELLATION BY BOOKING
    // =========================================================

    @GetMapping("/bookings/{bookingId}/cancellation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CancellationDTO>>
    getCancellationByBooking(
            @PathVariable String bookingId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        cancellationService
                                .getCancellationByBooking(
                                        bookingId
                                )
                )
        );
    }


    // =========================================================
    // GET CANCELLATION BY REFERENCE
    // =========================================================

    @GetMapping("/cancellations/reference/{cancellationReference}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CancellationDTO>>
    getCancellationByReference(
            @PathVariable String cancellationReference) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        cancellationService
                                .getCancellationByReference(
                                        cancellationReference
                                )
                )
        );
    }
}