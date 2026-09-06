package com.crimsonlogic.busticketbooking.entity;

import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Wallet {

    @Id
    @Column(name = "wallet_id")
    private String walletId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @PrePersist
    protected void generateId() {
        if (this.walletId == null) {
            this.walletId = EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_WALLET);
        }
    }
}
