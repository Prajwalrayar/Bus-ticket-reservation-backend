package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.*;
import com.crimsonlogic.busticketbooking.service.BusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buses")
@RequiredArgsConstructor
public class BusController {

    private final BusService busService;


    // =========================================================
    // BUS ENDPOINTS
    // =========================================================

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusDTO>>> getAllBuses() {
        return ResponseEntity.ok(ApiResponse.success(busService.getAllBuses()));
    }


    @GetMapping("/{registrationNumber}")
    public ResponseEntity<ApiResponse<BusDTO>> getBusByRegistrationNumber(
            @PathVariable String registrationNumber) {
        return ResponseEntity.ok(ApiResponse.success(busService.getBusByRegistrationNumber(registrationNumber)));
    }


    @GetMapping("/operator/{companyName}")
    public ResponseEntity<ApiResponse<List<BusDTO>>> getBusesByOperator(
            @PathVariable String companyName) {
        return ResponseEntity.ok(ApiResponse.success(busService.getBusesByOperator(companyName)));
    }


    @PostMapping
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<BusDTO>> createBus(@Valid @RequestBody BusCreateRequest request) {
        BusDTO createdBus = busService.createBus(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bus created successfully", createdBus));
    }


    @PutMapping("/{registrationNumber}")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<BusDTO>> updateBus(
            @PathVariable String registrationNumber,
            @Valid @RequestBody BusCreateRequest request) {
        BusDTO updatedBus = busService.updateBus(registrationNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Bus updated successfully", updatedBus));
    }


    // =========================================================
    // ACTIVATION REQUEST WORKFLOW
    // =========================================================

    /**
     * Operator requests reactivation of an inactive bus.
     */
    @PostMapping("/{registrationNumber}/request-activation")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> requestActivation(
            @PathVariable String registrationNumber,
            @Valid @RequestBody BusActivationRequestDTO request) {
        busService.requestActivation(registrationNumber, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(
                "Activation request submitted successfully. Awaiting admin review.", null));
    }


    /**
     * Admin views all buses with PENDING activation requests.
     */
    @GetMapping("/pending-activation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BusDTO>>> getPendingActivationRequests() {
        return ResponseEntity.ok(ApiResponse.success(busService.getPendingActivationRequests()));
    }


    /**
     * Admin approves the activation request and sets compensation fee.
     */
    @PostMapping("/{registrationNumber}/approve-activation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveActivationRequest(
            @PathVariable String registrationNumber,
            @Valid @RequestBody BusActivationApprovalRequest request) {
        busService.approveActivationRequest(registrationNumber, request.getCompensationAmount(), request.getAdminNote());
        return ResponseEntity.ok(ApiResponse.success(
                "Activation approved. Operator notified to pay compensation of ₹" + request.getCompensationAmount(), null));
    }


    /**
     * Admin rejects the activation request.
     */
    @PostMapping("/{registrationNumber}/reject-activation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectActivationRequest(
            @PathVariable String registrationNumber,
            @RequestBody(required = false) BusActivationApprovalRequest request) {
        String note = (request != null) ? request.getAdminNote() : null;
        busService.rejectActivationRequest(registrationNumber, note);
        return ResponseEntity.ok(ApiResponse.success("Activation request rejected.", null));
    }


    /**
     * Operator creates a Razorpay order to pay compensation fee.
     */
    @PostMapping("/{registrationNumber}/pay-compensation/razorpay/create-order")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createCompensationOrder(
            @PathVariable String registrationNumber) {
        RazorpayOrderResponse order = busService.createCompensationRazorpayOrder(registrationNumber);
        return ResponseEntity.ok(ApiResponse.success("Razorpay order created", order));
    }


    /**
     * Operator verifies Razorpay payment. On success, bus is reactivated.
     */
    @PostMapping("/{registrationNumber}/pay-compensation/razorpay/verify")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> verifyCompensationPayment(
            @PathVariable String registrationNumber,
            @Valid @RequestBody RazorpayVerificationRequest request) {
        busService.verifyCompensationAndActivate(registrationNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Payment successful! Bus has been reactivated.", null));
    }


    // =========================================================
    // BUS SEAT ENDPOINTS
    // =========================================================

    @GetMapping("/{registrationNumber}/seats")
    public ResponseEntity<ApiResponse<List<BusSeatDTO>>> getSeatsByBus(
            @PathVariable String registrationNumber) {
        return ResponseEntity.ok(ApiResponse.success(busService.getSeatsByBus(registrationNumber)));
    }


    @GetMapping("/{registrationNumber}/seats/{seatNumber}")
    public ResponseEntity<ApiResponse<BusSeatDTO>> getBusSeat(
            @PathVariable String registrationNumber,
            @PathVariable String seatNumber) {
        return ResponseEntity.ok(ApiResponse.success(busService.getBusSeat(registrationNumber, seatNumber)));
    }


    @PostMapping("/{registrationNumber}/seats")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<BusSeatDTO>> createBusSeat(
            @PathVariable String registrationNumber,
            @Valid @RequestBody BusSeatCreateRequest request) {
        BusSeatDTO createdSeat = busService.createBusSeat(registrationNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bus seat created successfully", createdSeat));
    }


    @PutMapping("/{registrationNumber}/seats/{seatNumber}")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<BusSeatDTO>> updateBusSeat(
            @PathVariable String registrationNumber,
            @PathVariable String seatNumber,
            @Valid @RequestBody BusSeatCreateRequest request) {
        BusSeatDTO updatedSeat = busService.updateBusSeat(registrationNumber, seatNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Bus seat updated successfully", updatedSeat));
    }


    @PatchMapping("/{registrationNumber}/seats/{seatNumber}/deactivate")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> deactivateBusSeat(
            @PathVariable String registrationNumber,
            @PathVariable String seatNumber) {
        busService.deactivateBusSeat(registrationNumber, seatNumber);
        return ResponseEntity.ok(ApiResponse.success("Bus seat deactivated successfully", null));
    }
}