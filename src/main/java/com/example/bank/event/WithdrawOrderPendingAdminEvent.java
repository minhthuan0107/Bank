package com.example.bank.event;

import com.example.bank.enums.wallet.Stablecoin;

import java.math.BigDecimal;

public record WithdrawOrderPendingAdminEvent(
        Long orderId,
        Long userId,
        String orderNo,
        Stablecoin currency,
        String network,
        String toAddress,
        BigDecimal amount
) {}