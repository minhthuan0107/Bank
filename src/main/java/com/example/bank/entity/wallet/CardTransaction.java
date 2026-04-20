package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import com.example.bank.enums.wallet.CardTransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "card_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CardTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "slash_transaction_id", nullable = false, length = 100)
    private String slashTransactionId;       // Slash "id" — upsert key

    @Column(name = "slash_event_id", length = 100)
    private String slashEventId;             // dedup webhook

    @Column(name = "card_id", length = 100)
    private String cardId;                   // String, không phải Long

    @Column(name = "user_id")
    private Long userId;                     // internal ID hệ thống bạn

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;                // âm = debit, dương = credit

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CardTransactionStatus status;    // PENDING / POSTED / FAILED

    @Column(name = "detailed_status", length = 30)
    private String detailedStatus;           // settled/declined/reversed/refund...

    @Column(name = "merchant_description", length = 255)
    private String merchantDescription;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "transaction_date")
    private Instant transactionDate;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "decline_reason", length = 255)
    private String declineReason;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;


}


