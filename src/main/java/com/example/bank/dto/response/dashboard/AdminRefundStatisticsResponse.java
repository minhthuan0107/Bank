package com.example.bank.dto.response.dashboard;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AdminRefundStatisticsResponse(
        long reversedCount,
        BigDecimal reversedAmount,
        BigDecimal reversedRate
) {
}
