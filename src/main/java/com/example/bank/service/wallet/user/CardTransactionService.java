package com.example.bank.service.wallet.user;

import com.example.bank.dto.response.wallet.user.CardTransactionPageResponse;

public interface CardTransactionService {
    CardTransactionPageResponse getUserCardTransactions(
            Long userId,
            int page
    );
}
