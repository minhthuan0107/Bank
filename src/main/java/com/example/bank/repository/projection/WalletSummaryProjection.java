package com.example.bank.repository.projection;

import java.math.BigDecimal;

public interface WalletSummaryProjection {
    BigDecimal getTotalBalance();

    BigDecimal getAllocatedBalance();

    BigDecimal getFrozenBalance();

    BigDecimal getAvailableBalance();
}
