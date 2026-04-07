package com.example.bank.repository.projection;

import java.math.BigDecimal;

public interface CardDashboardProjection {

    BigDecimal getTotalBalance();

    Long getActiveCount();

    Long getBlockedCount();
}