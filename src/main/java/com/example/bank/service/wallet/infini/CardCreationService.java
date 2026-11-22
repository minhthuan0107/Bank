package com.example.bank.service.wallet.infini;

import com.example.bank.dto.request.wallet.infini.CreateCardRequest;
import com.example.bank.dto.response.wallet.infini.CardCreationConfigResponse;
import com.example.bank.dto.response.wallet.infini.CardCreationResponse;

import java.util.UUID;

public interface CardCreationService {

    CardCreationResponse createCard(CreateCardRequest request, Long userId);

    CardCreationResponse getCreationStatus(UUID requestId, Long userId);

    CardCreationConfigResponse getCreationConfig(Long userId);

    void reconcilePendingOrders();

    void reconcile(String requestId);
}
