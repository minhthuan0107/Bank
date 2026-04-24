package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import com.example.bank.enums.wallet.CashbackStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_cashback_monthly")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCashbackMonthly extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Format: yyyy-MM (2026-04)
     */
    @Column(nullable = false, length = 7)
    private String month;

    /**
     * Cashback đang chờ duyệt
     */
    @Column(name = "total_spent", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalSpent;

    /**
     * Tổng tiền đã tiêu trong tháng
     */
    @Column(name = "cashback_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal cashbackAmount;

    @Column(name = "percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal percent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashbackStatus status;
    /**
     * Thời điểm admin duyệt
     */
    @Column(name = "approved_at")
    private Instant approvedAt;


}
