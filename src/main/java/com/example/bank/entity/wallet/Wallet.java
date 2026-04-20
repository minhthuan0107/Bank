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

    /**
     * Tổng tài sản thật
     */
    @Column(name = "total_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalBalance;

    /**
     * Tôổng số dư của thẻ
     */
    @Column(name = "allocated_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal allocatedBalance;

    /**
     * Số tiền bị đóng băng
     */
    @Column(name = "frozen_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal frozenBalance;

    /**
     * Số dư có thể dùng
     * = total - allocated - frozen
     */
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance;

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

