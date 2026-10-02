package com.example.bank.dto.response.wallet.user;

import com.example.bank.enums.wallet.CryptoNetwork;
import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class WithdrawSummaryResponse {

        @JsonProperty("wallet_id")
        private Long walletId;

        private BigDecimal balance;

        @JsonProperty("minimum_withdrawal_amount")
        private BigDecimal minimumWithdrawalAmount;

        private String currency;

        private CryptoNetwork network;

        private String address;
}
