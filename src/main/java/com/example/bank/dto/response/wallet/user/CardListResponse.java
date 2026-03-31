package com.example.bank.dto.response.wallet.user;

import com.example.bank.enums.wallet.CardBrand;
import com.example.bank.enums.wallet.CardStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class CardListResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("masked_card")
    private String maskedCard;

    @JsonProperty("brand")
    private CardBrand brand;

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


}