package com.example.bank.dto.response.wallet.user;

import com.example.bank.entity.wallet.CardTransaction;
import com.example.bank.enums.wallet.CardTransactionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class CardTransactionListResponse {

    private Long id;

    /**
     * Số tiền giao dịch
     * Âm = debit, dương = credit
     */
    private BigDecimal amount;

    private String currency;

    private CardTransactionStatus status;

    @JsonProperty("merchant_description")
    private String merchantDescription;

    @JsonProperty("slash_transaction_id")
    private String slashTransactionId;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    public static CardTransactionListResponse from(CardTransaction tx) {
        return CardTransactionListResponse.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .status(tx.getStatus())
                .merchantDescription(tx.getMerchantDescription())
                .slashTransactionId(tx.getSlashTransactionId())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }
}
