package com.example.bank.dto.response.wallet.infini;

import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.enums.wallet.CardStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class InfiniCardResponse {

    @JsonProperty("id")
    private Long id;

    @Builder.Default
    @JsonProperty("provider")
    private String provider = "INFINI";

    @JsonProperty("product_id")
    private Integer productId;

    @JsonProperty("masked_card")
    private String maskedCard;

    @JsonProperty("name")
    private String name;

    @JsonProperty("note")
    private String note;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("status")
    private CardStatus status;

    @JsonProperty("remaining_amount")
    private BigDecimal remainingAmount;

    @JsonProperty("created_at")
    private Instant createdAt;

    public static InfiniCardResponse from(InfiniCard card) {
        return InfiniCardResponse.builder()
                .id(card.getId())
                .productId(card.getProductId())
                .maskedCard(card.getLast4() == null ? "****" : "**** **** **** " + card.getLast4())
                .name(card.getName())
                .note(card.getNote())
                .currency(card.getCurrency())
                .status(card.getStatus())
                .remainingAmount(card.getRemainingAmount())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
