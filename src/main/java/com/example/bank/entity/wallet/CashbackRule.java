package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cashback_rules")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CashbackRule extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // min spending
    @Column(name = "min_spent", nullable = false, precision = 19, scale = 4)
    private BigDecimal minSpent;

    // max spending (có thể null = vô cực)
    @Column(name = "max_spent", precision = 19, scale = 4)
    private BigDecimal maxSpent;

    // % cashback (vd: 1.5 = 1.5%)
    @Column(name = "cashback_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal cashbackPercent;

    // active / disable
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // priority nếu overlap
    @Column(name = "priority")
    private Integer priority;

}