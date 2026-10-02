package com.example.bank.dto.response.dashboard;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AdminMoneyFlowStatisticsResponse(
        long successCount,
        BigDecimal   successAmount,

        long pendingCount,
        BigDecimal pendingAmount,

        long failedCount,
        BigDecimal failedAmount,

        BigDecimal successRate
) {
}
