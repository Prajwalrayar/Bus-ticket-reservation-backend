package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.WalletDTO;
import com.crimsonlogic.busticketbooking.dto.WalletRechargeRequest;
import com.crimsonlogic.busticketbooking.dto.WalletTransactionDTO;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.entity.Wallet;
import com.crimsonlogic.busticketbooking.entity.WalletTransaction;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.repository.WalletRepository;
import com.crimsonlogic.busticketbooking.repository.WalletTransactionRepository;
import com.crimsonlogic.busticketbooking.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    private String getCurrentUserEmail() {
        // Spring Security principal name = email (set in CustomUserDetailsService)
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Wallet getCurrentWallet() {
        String email = getCurrentUserEmail();
        return walletRepository.findByUser_UserEmail(email)
                .orElseGet(() -> {
                    // Auto-create wallet on first access for users who registered
                    // before wallet feature was added
                    User user = userRepository.findByUserEmail(email)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
                    Wallet newWallet = new Wallet();
                    newWallet.setUser(user);
                    newWallet.setBalance(BigDecimal.ZERO);
                    return walletRepository.save(newWallet);
                });
    }

    @Override
    @Transactional
    public WalletDTO getMyWallet() {
        Wallet wallet = getCurrentWallet();
        return convertToDTO(wallet);
    }

    @Override
    @Transactional
    public WalletDTO rechargeWallet(WalletRechargeRequest request) {
        Wallet wallet = getCurrentWallet();

        // Add balance
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        Wallet savedWallet = walletRepository.save(wallet);

        // Record transaction
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(savedWallet)
                .amount(request.getAmount())
                .transactionType("CREDIT")
                .description("UPI Recharge from " + request.getUpiId())
                .referenceId("UPI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();
        
        walletTransactionRepository.save(transaction);

        return convertToDTO(savedWallet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionDTO> getMyTransactions() {
        Wallet wallet = getCurrentWallet();
        return walletTransactionRepository.findByWallet_WalletIdOrderByCreatedAtDesc(wallet.getWalletId())
                .stream()
                .map(this::convertTransactionToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deductBalance(String userId, BigDecimal amount, String bookingId) {
        Wallet wallet = walletRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType("DEBIT")
                .description("Booking Payment for " + bookingId)
                .referenceId(bookingId)
                .build();
        
        walletTransactionRepository.save(transaction);
    }

    private WalletDTO convertToDTO(Wallet wallet) {
        return WalletDTO.builder()
                .walletId(wallet.getWalletId())
                .userId(wallet.getUser().getUserId())
                .balance(wallet.getBalance())
                .build();
    }

    private WalletTransactionDTO convertTransactionToDTO(WalletTransaction transaction) {
        return WalletTransactionDTO.builder()
                .transactionId(transaction.getTransactionId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .description(transaction.getDescription())
                .referenceId(transaction.getReferenceId())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
