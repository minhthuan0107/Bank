package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class CashbackDashboardResponse {

    @JsonProperty("total_spent")
    private BigDecimal totalSpent;

    @JsonProperty("cashback_amount")
    private BigDecimal cashbackAmount;

    @JsonProperty("current_percent")
    private BigDecimal currentPercent;

    private List<CashbackTierResponse> tiers;
}
