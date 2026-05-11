package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record UserCashFlowPointResponse(

        @JsonProperty("label")
        String label,

        @JsonProperty("deposit_amount")
        BigDecimal depositAmount,

        @JsonProperty("withdraw_amount")
        BigDecimal withdrawAmount
) {
}