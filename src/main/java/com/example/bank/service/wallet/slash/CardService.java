package com.example.bank.service.wallet.slash;

import com.example.bank.dto.response.wallet.user.CardDashboardResponse;
import com.example.bank.dto.response.wallet.user.CardPageResponse;
import com.example.bank.enums.wallet.CardStatus;
import java.time.Instant;

/** Legacy Slash card operations. */
public interface CardService {
    CardPageResponse getDashboard(Long userId, int page);

    void lockCard(Long userId, Long cardId);

    void unlockCard(Long userId, Long cardId);

    CardPageResponse getUserCards(
            Long userId,
            String cardNumber,
            String cardName,
            CardStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    );
    CardDashboardResponse getCardDashboard(Long userId);

}
