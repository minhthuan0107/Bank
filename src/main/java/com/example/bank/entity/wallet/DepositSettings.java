package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

    @Entity
    @Table(name = "deposit_settings")
    @Getter
    @Setter
    public class DepositSettings extends BaseEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        private Long id;

        @Column(name = "currency", nullable = false, length = 10, unique = true)
        private String currency;

        @Column(name = "fee_percent", nullable = false, precision = 5, scale = 2)
        private BigDecimal feePercent;

        @Column(name = "min_amount", nullable = false, precision = 19, scale = 4)
        private BigDecimal minAmount;

        @Column(name = "max_amount", precision = 19, scale = 4)
        private BigDecimal maxAmount;

        @Column(name = "status", length = 20)
        private String status;
    }

