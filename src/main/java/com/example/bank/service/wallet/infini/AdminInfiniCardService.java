package com.example.bank.service.wallet.infini;

import com.example.bank.dto.response.wallet.infini.AdminInfiniCardPageResponse;
import com.example.bank.enums.wallet.CardStatus;

import java.time.Instant;

/** Admin view and lock/unlock of Infini cards of any user. */
public interface AdminInfiniCardService {

    AdminInfiniCardPageResponse getCards(String cardNumber, String cardName, String username,
                                         CardStatus status, Instant fromTime, Instant toTime, int page);

    void lockCard(Long adminId, Long cardId);

    void unlockCard(Long adminId, Long cardId);
}
