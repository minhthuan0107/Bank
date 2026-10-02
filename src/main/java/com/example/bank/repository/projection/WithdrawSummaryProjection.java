package com.example.bank.repository.projection;

import com.example.bank.enums.wallet.CryptoNetwork;
import com.example.bank.enums.wallet.Stablecoin;

import java.math.BigDecimal;

public interface WithdrawSummaryProjection {

    Long getWalletId();

    BigDecimal getBalance();

    BigDecimal getMinimumWithdrawalAmount();

    String getCurrency();

    String getNetwork();

    String getAddress();
}
