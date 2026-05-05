package com.example.bank.dto.response.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CashFlowPointResponse {

    /**
     * WEEK: Mon, Tue...
     * MONTH: 01, 02...
     * YEAR: Jan, Feb...
     */
    private String label;

    @JsonProperty("deposit_amount")
    private BigDecimal depositAmount;

    @JsonProperty("withdraw_amount")
    private BigDecimal withdrawAmount;
}