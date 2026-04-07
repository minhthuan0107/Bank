package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CardDashboardResponse {

    @JsonProperty("total_balance")
    private BigDecimal totalBalance;

    @JsonProperty("active_count")
    private Long activeCount;

    @JsonProperty("blocked_count")
    private Long blockedCount;
}
