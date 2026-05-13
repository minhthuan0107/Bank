package com.example.bank.dto.response.wallet.user;

import com.example.bank.enums.wallet.CardTxnStatus;
import com.example.bank.enums.wallet.CardTxnType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record CardFundingTransactionItemResponse(

        @JsonProperty("id")
        Long id,

        @JsonProperty("card_id")
        String cardId,

        @JsonProperty("type")
        CardTxnType type,

        @JsonProperty("amount")
        BigDecimal amount,

        @JsonProperty("status")
        CardTxnStatus status,

        @JsonProperty("created_at")
        Instant createdAt
) {
}
