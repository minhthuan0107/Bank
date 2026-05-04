package com.example.bank.service.wallet.admin;

import com.example.bank.dto.response.wallet.admin.AdminCardPageResponse;
import com.example.bank.enums.wallet.CardStatus;

import java.time.Instant;

public interface CardAdminService {
    AdminCardPageResponse getAdminCards(
            String cardNumber,
            String cardName,
            String username,
            CardStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    );

    void lockCard(Long cardId);

    void unlockCard(Long cardId);
}
