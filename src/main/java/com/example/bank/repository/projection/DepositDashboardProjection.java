package com.example.bank.repository.projection;

import java.math.BigDecimal;

public interface DepositDashboardProjection {
    BigDecimal getTotalAmount();

    Long getPendingCount();

    Long getSuccessCount();

    Long getFailedCount();
}
