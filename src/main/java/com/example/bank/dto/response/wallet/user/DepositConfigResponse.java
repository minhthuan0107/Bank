package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DepositConfigResponse {

    @JsonProperty("available_balance")
    private BigDecimal availableBalance;

    @JsonProperty("fee_percent")
    private BigDecimal feePercent;

    @JsonProperty("min_amount")
    private BigDecimal minAmount;

    @JsonProperty("max_amount")
    private BigDecimal maxAmount;

}
