package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.PaymentDTO;
import com.crimsonlogic.busticketbooking.dto.PaymentRequest;

import java.util.List;

public interface PaymentService {

    PaymentDTO initiatePayment(
            String bookingId,
            PaymentRequest request
    );

    PaymentDTO mockCheckout(
            String bookingId,
            PaymentRequest request
    );

    com.crimsonlogic.busticketbooking.dto.RazorpayOrderResponse createRazorpayOrder(
            String bookingId,
            PaymentRequest request
    );

    PaymentDTO verifyRazorpayPayment(
            String bookingId,
            com.crimsonlogic.busticketbooking.dto.RazorpayVerificationRequest request
    );

    PaymentDTO getPaymentById(
            String paymentId
    );

    PaymentDTO getPaymentByTransactionReference(
            String transactionReference
    );

    List<PaymentDTO> getPaymentsByBooking(
            String bookingId
    );
}
