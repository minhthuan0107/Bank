package com.example.bank.repository.projection;

import java.math.BigDecimal;

public interface WithdrawDashboardProjection {

    BigDecimal getTotalAmount();

    Long getSuccessCount();

    Long getFailedCount();
}
