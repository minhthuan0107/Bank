package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record UserAssetAllocationItemResponse(

        @JsonProperty("key")
        String key,

        @JsonProperty("amount")
        BigDecimal amount,

        @JsonProperty("percent")
        BigDecimal percent
) {
}
