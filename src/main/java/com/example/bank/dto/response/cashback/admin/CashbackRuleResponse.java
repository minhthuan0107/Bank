package com.example.bank.dto.response.cashback.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
public class CashbackRuleResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("min_spent")
    private BigDecimal minSpent;

    @JsonProperty("max_spent")
    private BigDecimal maxSpent;

    @JsonProperty("cashback_percent")
    private BigDecimal cashbackPercent;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}