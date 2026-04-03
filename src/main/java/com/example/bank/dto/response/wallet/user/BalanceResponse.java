package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public class BalanceResponse {

    @JsonProperty("wallet_balance")
    private BigDecimal walletBalance;

    @JsonProperty("card_balance")
    private BigDecimal cardBalance;

}
