package com.example.bank.dto.response.cashback.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;

@Builder
public record UserProfileResponse(

        @JsonProperty("username")
        String username,

        @JsonProperty("email")
        String email,

        @JsonProperty("created_at")
        Instant createdAt,

        @JsonProperty("card_open_limit")
        Integer cardOpenLimit
) {
}