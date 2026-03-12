package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import com.example.bank.enums.wallet.Stablecoin;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "wallets")
@Getter
@Setter
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Stablecoin currency;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "frozen_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal frozenBalance;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}

