package com.example.bank.repository.projection;

import com.example.bank.enums.wallet.DepositOrderStatus;
import com.example.bank.enums.wallet.Stablecoin;

import java.math.BigDecimal;
import java.time.Instant;

public interface AdminDepositHistoryProjection {

    String getOrderNo();

    Stablecoin getCurrency();

    String getNetwork();

    String getAddress();

    BigDecimal getAmount();

    BigDecimal getFee();

    BigDecimal getExpectedAmount();

    DepositOrderStatus getStatus();

    String getAdminNote();

    Instant getCreatedAt();

    Instant getUpdatedAt();
}