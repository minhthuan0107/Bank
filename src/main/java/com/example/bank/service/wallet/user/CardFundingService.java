package com.example.bank.service.wallet.user;

import com.example.bank.dto.response.wallet.user.CardFundingTransactionPageResponse;
import com.example.bank.enums.wallet.CardTxnStatus;
import com.example.bank.enums.wallet.CardTxnType;

import java.math.BigDecimal;
import java.time.Instant;

public interface CardFundingService {
    void topupCard (Long userId,
                   Long cardId,
                   BigDecimal amount,
                   String referenceId
    );

    void withdrawCard (Long userId,
                    Long cardId,
                    BigDecimal amount,
                    String referenceId
    );

    CardFundingTransactionPageResponse getUserFundingTransactions(
            Long userId,
            String last4,
            CardTxnType type,
            CardTxnStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    );
}
