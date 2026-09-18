package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.WalletDTO;
import com.crimsonlogic.busticketbooking.dto.WalletRechargeRequest;
import com.crimsonlogic.busticketbooking.dto.WalletTransactionDTO;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    WalletDTO getMyWallet();
    WalletDTO rechargeWallet(WalletRechargeRequest request);
    List<WalletTransactionDTO> getMyTransactions();
    
    // Internal use for booking
    void deductBalance(String userId, BigDecimal amount, String bookingId);
    
    // Internal use for refunds
    void addBalance(String userId, BigDecimal amount, String description, String referenceId);

    // Razorpay Integration
    com.crimsonlogic.busticketbooking.dto.RazorpayOrderResponse createRazorpayOrder(WalletRechargeRequest request);
    WalletDTO verifyRazorpayPayment(com.crimsonlogic.busticketbooking.dto.RazorpayVerificationRequest request);
}
