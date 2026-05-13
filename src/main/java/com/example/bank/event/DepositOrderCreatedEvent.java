package com.example.bank.event;

import com.example.bank.enums.wallet.Stablecoin;

import java.math.BigDecimal;

public record DepositOrderCreatedEvent(
        Long orderId,
        Long userId,
        String orderNo,
        Stablecoin currency,
        String network,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal expectedAmount,
        String depositAddress
) {
}