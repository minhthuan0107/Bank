package com.example.bank.service.wallet.infini;

import com.example.bank.dto.response.wallet.infini.InfiniCardPageResponse;
import com.example.bank.dto.response.wallet.infini.InfiniCardResponse;

public interface InfiniCardService {

    InfiniCardPageResponse getCards(Long userId, int page);

    InfiniCardResponse getCard(Long userId, Long cardId);
}
