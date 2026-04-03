package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import com.example.bank.enums.wallet.CardTransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;


@Entity
@Table(name = "card_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // ===== EXTERNAL (idempotent key) =====
    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    // ===== RELATION =====
    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // ===== AMOUNT =====
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency;

    // ===== STATUS =====
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardTransactionStatus status;

    // ===== MERCHANT INFO =====
    @Column(name = "merchant_name", length = 255)
    private String merchantName;

    @Column(name = "merchant_category", length = 100)
    private String merchantCategory;

    @Column(name = "description", length = 255)
    private String description;

    // ===== RAW PAYLOAD =====
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;
}



