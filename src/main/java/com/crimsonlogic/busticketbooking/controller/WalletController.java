package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.WalletDTO;
import com.crimsonlogic.busticketbooking.dto.WalletRechargeRequest;
import com.crimsonlogic.busticketbooking.dto.WalletTransactionDTO;
import com.crimsonlogic.busticketbooking.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/my-wallet")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WalletDTO>> getMyWallet() {
        return ResponseEntity.ok(ApiResponse.success(walletService.getMyWallet()));
    }

    @PostMapping("/recharge")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WalletDTO>> rechargeWallet(
            @Valid @RequestBody WalletRechargeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Wallet recharged successfully",
                walletService.rechargeWallet(request)
        ));
    }

    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<WalletTransactionDTO>>> getMyTransactions() {
        return ResponseEntity.ok(ApiResponse.success(walletService.getMyTransactions()));
    }
}
