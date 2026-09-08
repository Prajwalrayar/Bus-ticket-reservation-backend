package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, String> {
    List<WalletTransaction> findByWallet_WalletIdOrderByCreatedAtDesc(String walletId);
}
