package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.PaymentDTO;
import com.crimsonlogic.busticketbooking.dto.PaymentRequest;
import com.crimsonlogic.busticketbooking.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiate payment (creates an INITIATED payment record).
     */
    @PostMapping("/bookings/{bookingId}/payments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentDTO>> initiatePayment(
            @PathVariable String bookingId,
            @Valid @RequestBody PaymentRequest request) {

        PaymentDTO payment = paymentService.initiatePayment(bookingId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated", payment));
    }

    /**
     * Mock checkout — simulates the full payment gateway flow.
     * 80% chance of SUCCESS.  On success, booking is CONFIRMED and seats are BOOKED.
     * On failure, seat locks are released.
     */
    @PostMapping("/payments/mock-checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentDTO>> mockCheckout(
            @RequestParam String bookingId,
            @Valid @RequestBody PaymentRequest request) {

        PaymentDTO payment = paymentService.mockCheckout(bookingId, request);
        return ResponseEntity.ok(ApiResponse.success(
                payment.getPaymentStatus().name().equals("SUCCESS")
                        ? "Payment successful. Booking confirmed!"
                        : "Payment failed. Please try again.",
                payment));
    }

    /**
     * Get all payment attempts for a booking.
     */
    @GetMapping("/bookings/{bookingId}/payments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PaymentDTO>>> getPaymentsByBooking(
            @PathVariable String bookingId) {

        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentsByBooking(bookingId)));
    }

    /**
     * Get payment by internal payment ID.
     */
    @GetMapping("/payments/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentById(
            @PathVariable String paymentId) {

        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentById(paymentId)));
    }

    /**
     * Get payment by transaction reference (customer-facing).
     */
    @GetMapping("/payments/reference/{transactionReference}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPaymentByReference(
            @PathVariable String transactionReference) {

        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentByTransactionReference(transactionReference)));
    }
}