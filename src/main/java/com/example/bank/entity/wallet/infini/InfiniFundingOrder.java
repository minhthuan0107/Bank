package com.example.bank.entity.wallet.infini;

import com.example.bank.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** A top-up or redeem of an Infini card, keyed by the client's request ID. */
@Entity
@Table(name = "infini_funding_orders")
@Getter
@Setter
public class InfiniFundingOrder extends BaseEntity {
    public static final String TOP_UP = "TOP_UP";
    public static final String REDEEM = "REDEEM";

    public static final String PENDING = "PENDING";
    public static final String REVIEW_REQUIRED = "REVIEW_REQUIRED";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";

    @Id
    @Column(length = 36)
    private String requestId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long walletId;

    // Local ID in cards_infini.
    @Column(name = "infini_card_id", nullable = false)
    private Long cardId;

    @Column(nullable = false, length = 100)
    private String providerCardId;

    @Column(nullable = false, length = 10)
    private String type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String tokenType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 100)
    private String reviewReason;

    @Column(unique = true, length = 100)
    private String providerTxId;

    @Column(precision = 19, scale = 4)
    private BigDecimal providerCardBalance;

    // Set right before the provider request is sent; null means it was never sent.
    @Column
    private Instant submitStartedAt;

    public boolean isUnresolved() {
        return PENDING.equals(status) || REVIEW_REQUIRED.equals(status);
    }
}
