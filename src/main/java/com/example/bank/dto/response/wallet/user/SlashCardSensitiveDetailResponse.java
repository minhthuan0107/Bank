package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record SlashCardSensitiveDetailResponse(

        @JsonProperty("id")
        String id,

        @JsonProperty("pan")
        String pan,

        @JsonProperty("expiryYear")
        Integer expiryYear,

        @JsonProperty("expiryMonth")
        Integer expiryMonth,

        @JsonProperty("createdAt")
        Instant createdAt,

        @JsonProperty("cvv")
        String cvv
) {
}