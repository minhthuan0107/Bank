package com.example.bank.dto.response.wallet.user;

import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record WithdrawSummaryResponse(

        @JsonProperty("wallet_id")
        Long walletId,

        @JsonProperty("balance")
        BigDecimal balance,

        @JsonProperty("minimum_withdrawal_amount")
        BigDecimal minimumWithdrawalAmount,

        @JsonProperty("currency")
        Stablecoin currency
) {
}
