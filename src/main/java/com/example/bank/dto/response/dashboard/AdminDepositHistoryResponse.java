package com.example.bank.dto.response.dashboard;

import com.example.bank.enums.wallet.DepositOrderStatus;
import com.example.bank.enums.wallet.Stablecoin;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminDepositHistoryResponse(
        String orderNo,
        Stablecoin currency,
        String network,
        String address,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal expectedAmount,
        DepositOrderStatus status,
        String adminNote,
        Instant createdAt,
        Instant updatedAt
) {
}
