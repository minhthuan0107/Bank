package com.example.bank.dto.response.wallet.infini;

import com.example.bank.entity.wallet.infini.CardCreationOrder;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CardCreationResponse {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("status")
    private String status;

    @Builder.Default
    @JsonProperty("provider")
    private String provider = "INFINI";

    @JsonProperty("card_id")
    private Long cardId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("fee")
    private BigDecimal fee;

    @JsonProperty("total_debit")
    private BigDecimal totalDebit;

    public static CardCreationResponse from(CardCreationOrder order) {
        return CardCreationResponse.builder()
                .requestId(order.getRequestId())
                .status(order.getStatus())
                .cardId(order.getCardId())
                .amount(order.getAmount())
                .fee(order.getFee())
                .totalDebit(order.getTotalDebit())
                .build();
    }
}
