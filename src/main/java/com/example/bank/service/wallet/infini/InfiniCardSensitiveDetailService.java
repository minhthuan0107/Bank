package com.example.bank.service.wallet.infini;

import com.example.bank.dto.response.wallet.user.CardSensitiveDetailResponse;

/** Full card number, CVV and expiry of an Infini card, read from Infini on every request and never stored. */
public interface InfiniCardSensitiveDetailService {

    CardSensitiveDetailResponse getSensitiveDetail(Long userId, Long cardId);
}
