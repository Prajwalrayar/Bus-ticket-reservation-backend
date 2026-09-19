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
import org.springframework.beans.factory.annotation.Value;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

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

    @Override
    @Transactional
    public void addBalance(String userId, BigDecimal amount, String description, String referenceId) {
        Wallet wallet = walletRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType("CREDIT")
                .description(description)
                .referenceId(referenceId)
                .build();
        
        walletTransactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public com.crimsonlogic.busticketbooking.dto.RazorpayOrderResponse createRazorpayOrder(WalletRechargeRequest request) {
        Wallet wallet = getCurrentWallet();

        com.crimsonlogic.busticketbooking.dto.RazorpayOrderResponse response = new com.crimsonlogic.busticketbooking.dto.RazorpayOrderResponse();

        if (request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            try {
                JSONObject orderRequest = new JSONObject();
                // amount in paise
                orderRequest.put("amount", request.getAmount().multiply(new BigDecimal("100")).intValue());
                orderRequest.put("currency", "INR");
                orderRequest.put("receipt", "WALLET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

                Order razorpayOrder = razorpayClient.orders.create(orderRequest);
                
                response.setOrderId(razorpayOrder.get("id"));
                response.setKeyId(razorpayKeyId);
                response.setAmount(request.getAmount());
                response.setCurrency("INR");
                
                // Record INITIATED transaction
                WalletTransaction transaction = WalletTransaction.builder()
                        .wallet(wallet)
                        .amount(request.getAmount())
                        .transactionType("INITIATED")
                        .description("Initiated Wallet Recharge")
                        .referenceId(razorpayOrder.get("id"))
                        .build();
                walletTransactionRepository.save(transaction);

            } catch (RazorpayException e) {
                log.error("Failed to create Razorpay Order for wallet recharge", e);
                throw new RuntimeException("Failed to create Razorpay Order: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        return response;
    }

    @Override
    @Transactional
    public WalletDTO verifyRazorpayPayment(com.crimsonlogic.busticketbooking.dto.RazorpayVerificationRequest request) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, razorpaySecret);

            if (isValid) {
                // Find initiated transaction
                WalletTransaction transaction = walletTransactionRepository.findByWallet_WalletIdOrderByCreatedAtDesc(getCurrentWallet().getWalletId())
                        .stream()
                        .filter(t -> "INITIATED".equals(t.getTransactionType()) && request.getRazorpayOrderId().equals(t.getReferenceId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Initiated transaction not found for this order ID"));

                Wallet wallet = transaction.getWallet();
                wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
                Wallet savedWallet = walletRepository.save(wallet);

                transaction.setTransactionType("CREDIT");
                transaction.setDescription("Wallet Recharge via Razorpay");
                transaction.setReferenceId(request.getRazorpayPaymentId());
                walletTransactionRepository.save(transaction);

                log.info("Razorpay Payment verified for wallet recharge. User ID: {}", wallet.getUser().getUserId());
                return convertToDTO(savedWallet);
            } else {
                log.warn("Razorpay signature verification failed for wallet recharge.");
                throw new IllegalArgumentException("Signature Verification Failed");
            }

        } catch (Exception e) {
            log.error("Razorpay signature verification failed for wallet recharge", e);
            throw new IllegalArgumentException("Verification Exception: " + e.getMessage());
        }
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
