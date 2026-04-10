package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class WithdrawDashboardResponse {

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("success_count")
    private Long successCount;

    @JsonProperty("failed_count")
    private Long failedCount;
}