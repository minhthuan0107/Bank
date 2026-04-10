package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DashboardResponse {
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("total_balance")
    private BigDecimal totalBalance;

    @JsonProperty("available_balance")
    private BigDecimal availableBalance;

    @JsonProperty("frozen_balance")
    private BigDecimal frozenBalance;

    @JsonProperty("allocated_balance")
    private BigDecimal allocatedBalance;

    @JsonProperty("card_opening_limit")
    private Integer cardOpeningLimit;

    @JsonProperty("activated_card_count")
    private Long activatedCardCount;
}

