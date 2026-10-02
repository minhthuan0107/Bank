package com.example.bank.repository.projection;

import java.math.BigDecimal;

public interface AdminMoneyFlowStatisticsProjection {

    Long getSuccessCount();

    BigDecimal getSuccessAmount();

    Long getPendingCount();

    BigDecimal getPendingAmount();

    Long getFailedCount();

    BigDecimal getFailedAmount();
}