package com.example.bank.service.wallet.slash;

import com.example.bank.dto.response.wallet.user.CardSensitiveDetailResponse;

public interface CardSensitiveDetailService {

    CardSensitiveDetailResponse getSensitiveDetail(Long userId, Long cardId);
}
