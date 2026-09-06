package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.BookingCancelRequest;
import com.crimsonlogic.busticketbooking.dto.BookingCreateRequest;
import com.crimsonlogic.busticketbooking.dto.BookingDTO;
import com.crimsonlogic.busticketbooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;


    /*
     * Create booking.
     *
     * Only authenticated users should create
     * passenger bookings.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingDTO>> createBooking(
            @Valid @RequestBody BookingCreateRequest request) {

        BookingDTO booking =
                bookingService.createBooking(
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Booking created successfully",
                                booking
                        )
                );
    }


    /*
     * Get booking by internal booking ID.
     */
    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingDTO>> getBookingById(
            @PathVariable String bookingId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        bookingService.getBookingById(
                                bookingId
                        )
                )
        );
    }


    /*
     * Get booking using customer-facing
     * booking reference.
     *
     * Example:
     * BK-7F82QX
     */
    @GetMapping("/reference/{bookingReference}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingDTO>>
    getBookingByReference(
            @PathVariable String bookingReference) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        bookingService.getBookingByReference(
                                bookingReference
                        )
                )
        );
    }


    /*
     * Get current authenticated user's bookings.
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BookingDTO>>> getMyBookings() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        bookingService.getMyBookings()
                )
        );
    }

    /*
     * Get all bookings (Admin only).
     */
    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUS_OPERATOR', 'SUPPORT_AGENT')")
    public ResponseEntity<ApiResponse<List<BookingDTO>>> getAllBookings() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        bookingService.getAllBookings()
                )
        );
    }


    /*
     * Cancel booking.
     */
    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingDTO>>
    cancelBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody BookingCancelRequest request) {

        BookingDTO cancelledBooking =
                bookingService.cancelBooking(
                        bookingId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Booking cancelled successfully",
                        cancelledBooking
                )
        );
    }


    /*
     * Cancel booking by reference number.
     */
    @PostMapping("/reference/{bookingReference}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingDTO>>
    cancelBookingByReference(
            @PathVariable String bookingReference,
            @Valid @RequestBody BookingCancelRequest request) {

        BookingDTO booking =
                bookingService.getBookingByReference(
                        bookingReference
                );

        BookingDTO cancelledBooking =
                bookingService.cancelBooking(
                        booking.getBookingId(),
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Booking cancelled successfully",
                        cancelledBooking
                )
        );
    }
}