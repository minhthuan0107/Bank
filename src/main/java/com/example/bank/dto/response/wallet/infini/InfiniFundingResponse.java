package com.example.bank.dto.response.wallet.infini;

import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InfiniFundingResponse {

    @JsonProperty("request_id")
    private String requestId;

    // TOP_UP or REDEEM
    @JsonProperty("type")
    private String type;

    // PENDING, REVIEW_REQUIRED, SUCCEEDED or FAILED
    @JsonProperty("status")
    private String status;

    // Local ID in cards_infini.
    @JsonProperty("card_id")
    private Long cardId;

    @JsonProperty("amount")
    private BigDecimal amount;

    // Card balance reported by Infini after the operation, when known.
    @JsonProperty("card_balance")
    private BigDecimal cardBalance;

    public static InfiniFundingResponse from(InfiniFundingOrder order) {
        return InfiniFundingResponse.builder()
                .requestId(order.getRequestId())
                .type(order.getType())
                .status(order.getStatus())
                .cardId(order.getCardId())
                .amount(order.getAmount())
                .cardBalance(order.getProviderCardBalance())
                .build();
    }
}
