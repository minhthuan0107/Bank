package com.example.bank.dto.response.wallet.admin;

import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record WalletCurrencySettingsResponse(
        Long id,

        Stablecoin currency,

        @JsonProperty("deposit_fee_percent")
        BigDecimal depositFeePercent,

        @JsonProperty("min_deposit_amount")
        BigDecimal minDepositAmount,

        @JsonProperty("min_withdraw_amount")
        BigDecimal minWithdrawAmount,

        @JsonProperty("min_card_funding_amount")
        BigDecimal minCardFundingAmount,

        String status
) {
}
