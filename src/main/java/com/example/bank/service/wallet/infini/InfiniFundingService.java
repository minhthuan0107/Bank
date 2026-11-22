package com.example.bank.service.wallet.infini;

import com.example.bank.dto.request.wallet.infini.InfiniFundingRequest;
import com.example.bank.dto.response.wallet.infini.InfiniFundingResponse;

import java.util.UUID;

public interface InfiniFundingService {

    InfiniFundingResponse topUp(Long userId, Long cardId, InfiniFundingRequest request);

    InfiniFundingResponse withdraw(Long userId, Long cardId, InfiniFundingRequest request);

    InfiniFundingResponse getStatus(UUID requestId, Long userId);

    /** Releases never-sent orders and flags interrupted ones. Called by the scheduler. */
    void expireStaleOrders();
}
