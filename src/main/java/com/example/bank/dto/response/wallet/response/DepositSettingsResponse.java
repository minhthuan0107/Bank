package com.example.bank.dto.response.wallet.response;

import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DepositSettingsResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("currency")
    private Stablecoin currency;

    @JsonProperty("fee_percent")
    private BigDecimal feePercent;

    @JsonProperty("min_amount")
    private BigDecimal minAmount;

    @JsonProperty("max_amount")
    private BigDecimal maxAmount;

    @JsonProperty("status")
    private String status;

}