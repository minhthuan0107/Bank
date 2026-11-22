package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "card_creation_orders")
@Getter
@Setter
public class CardCreationOrder extends BaseEntity {
    @Id
    @Column(length = 36)
    private String requestId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long walletId;

    @Column(nullable = false)
    private Integer productId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String holderName;

    @Column(name = "member_email", nullable = false, length = 255)
    private String applicantEmail;

    @Column(length = 255)
    private String note;

    @Column(nullable = false, length = 10)
    private String tokenType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDebit;

    @Column(precision = 19, scale = 4)
    private BigDecimal providerFee;

    @Column(precision = 19, scale = 4)
    private BigDecimal providerTotalPay;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(unique = true, length = 100)
    private String providerCardId;

    // Local ID in cards_infini, not cards.
    @Column(name = "infini_card_id")
    private Long cardId;

    @Column(length = 100)
    private String reviewReason;

    @Column(nullable = false)
    private Instant nextCheckAt;

    // Set right before the apply request is sent; null means it was never sent.
    @Column
    private Instant applyStartedAt;
}
