package com.example.bank.service.wallet.user;

import java.math.BigDecimal;

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
}
