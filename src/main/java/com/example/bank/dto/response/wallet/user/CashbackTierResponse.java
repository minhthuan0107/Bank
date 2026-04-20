package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CashbackTierResponse {

    @JsonProperty("min_spent")
    private BigDecimal minSpent;

    @JsonProperty("max_spent")
    private BigDecimal maxSpent;

    private BigDecimal percent;

    @JsonProperty("is_current")
    private Boolean isCurrent;
}