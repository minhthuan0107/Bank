package com.example.bank.projection;

import java.math.BigDecimal;

public interface WalletAssetAllocationProjection {

    BigDecimal getTotalBalance();

    BigDecimal getAllocatedBalance();

    BigDecimal getFrozenBalance();

    BigDecimal getAvailableBalance();
}
