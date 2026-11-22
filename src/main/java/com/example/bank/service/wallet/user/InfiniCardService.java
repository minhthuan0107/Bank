package com.example.bank.service.wallet.user;

import com.example.bank.dto.response.wallet.user.InfiniCardPageResponse;
import com.example.bank.dto.response.wallet.user.InfiniCardResponse;

public interface InfiniCardService {

    InfiniCardPageResponse getCards(Long userId, int page);

    InfiniCardResponse getCard(Long userId, Long cardId);
}
