package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;

@Builder
public record CardSensitiveDetailResponse(

        @JsonProperty("pan")
        String pan,

        @JsonProperty("expiry_year")
        Integer expiryYear,

        @JsonProperty("expiry_month")
        Integer expiryMonth,

        @JsonProperty("created_at")
        Instant createdAt,

        @JsonProperty("cvv")
        String cvv
) {
}