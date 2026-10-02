package com.example.bank.repository.projection;

import java.math.BigDecimal;

public interface AdminCardTransactionStatisticsProjection {

    Long getPostedCount();

    BigDecimal getPostedAmount();

    Long getPendingCount();

    BigDecimal getPendingAmount();

    Long getFailedCount();

    BigDecimal getFailedAmount();

    Long getReversedCount();

    BigDecimal getReversedAmount();
}
