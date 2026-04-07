package com.example.bank.service.wallet.user;

import com.example.bank.dto.request.wallet.user.CreateCardRequest;
import com.example.bank.dto.response.wallet.user.BalanceResponse;
import com.example.bank.dto.response.wallet.user.CardDashboardResponse;
import com.example.bank.dto.response.wallet.user.CardPageResponse;
import com.example.bank.enums.wallet.CardStatus;

import java.time.Instant;

public interface CardService {
    void createCard(CreateCardRequest request, Long userId) ;

    CardPageResponse getDashboard(Long userId, int page);

    BalanceResponse getUserBalance(Long userId) ;

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
