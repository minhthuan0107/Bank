package com.example.bank.repository.projection;

import java.math.BigDecimal;

public interface AdminDepositStatisticsProjection {

    Long getSuccessCount();

    BigDecimal getSuccessAmount();

    Long getPendingCount();

    BigDecimal getPendingAmount();

    Long getFailedCount();

    BigDecimal getFailedAmount();
}