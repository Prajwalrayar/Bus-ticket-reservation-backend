package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.*;

import java.math.BigDecimal;
import java.util.List;

public interface BusService {

    // Bus operations
    BusDTO createBus(BusCreateRequest request);

    BusDTO getBusByRegistrationNumber(String registrationNumber);

    List<BusDTO> getAllBuses();

    List<BusDTO> getBusesByOperator(String companyName);

    BusDTO updateBus(
            String registrationNumber,
            BusCreateRequest request
    );

    // ── Activation Request Workflow ──────────────────────────────────────────

    /** Operator submits a reactivation request for an inactive bus. */
    void requestActivation(String registrationNumber, String reason);

    /** Admin approves the request and sets compensation fee. */
    void approveActivationRequest(String registrationNumber, BigDecimal compensationAmount, String adminNote);

    /** Admin rejects the request. */
    void rejectActivationRequest(String registrationNumber, String adminNote);

    /** Admin view: all buses with a PENDING activation request. */
    List<BusDTO> getPendingActivationRequests();

    /** Create a Razorpay order for the operator to pay compensation. */
    RazorpayOrderResponse createCompensationRazorpayOrder(String registrationNumber);

    /** Verify Razorpay payment and activate bus. */
    void verifyCompensationAndActivate(String registrationNumber, RazorpayVerificationRequest request);

    // ── Auto-deactivation (called by scheduler) ──────────────────────────────
    void autoDeactivateInactiveBuses();

    // Physical seat configuration operations
    BusSeatDTO createBusSeat(
            String busRegistrationNumber,
            BusSeatCreateRequest request
    );

    BusSeatDTO getBusSeat(
            String busRegistrationNumber,
            String seatNumber
    );

    List<BusSeatDTO> getSeatsByBus(
            String busRegistrationNumber
    );

    BusSeatDTO updateBusSeat(
            String busRegistrationNumber,
            String seatNumber,
            BusSeatCreateRequest request
    );

    void deactivateBusSeat(
            String busRegistrationNumber,
            String seatNumber
    );
}