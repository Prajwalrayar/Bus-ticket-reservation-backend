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
}
