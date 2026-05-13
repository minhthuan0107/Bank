package com.example.bank.repository.projection;

import com.example.bank.enums.wallet.Stablecoin;

import java.math.BigDecimal;

public interface WithdrawSummaryProjection {

    Long getWalletId();

    BigDecimal getBalance();


    BigDecimal getMinimumWithdrawalAmount();

    Stablecoin getCurrency();
}
