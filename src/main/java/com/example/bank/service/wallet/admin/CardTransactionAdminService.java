package com.example.bank.service.wallet.admin;

import com.example.bank.dto.response.wallet.user.CardTransactionPageResponse;
import com.example.bank.enums.wallet.CardTransactionStatus;

import java.time.Instant;

public interface CardTransactionAdminService {
    CardTransactionPageResponse getAdminUserCardTransactions(
            Long userId,
            String slashTransactionId,
            String merchantDescription,
            CardTransactionStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    );

    CardTransactionPageResponse getUserCardTransactions(
            Long userId,
            int page
    );
}
