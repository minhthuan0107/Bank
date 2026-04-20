package com.example.bank.entity.wallet;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cashback_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashbackTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "cashback_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal cashbackAmount;

    @Column(name = "cashback_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal cashbackPercent;

    @Column(name = "created_at")
    private Instant createdAt;
}
