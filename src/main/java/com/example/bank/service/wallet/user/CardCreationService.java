package com.example.bank.service.wallet.user;

import com.example.bank.dto.request.wallet.user.CreateCardRequest;
import com.example.bank.dto.response.wallet.user.CardCreationConfigResponse;
import com.example.bank.dto.response.wallet.user.CardCreationResponse;

import java.util.UUID;

public interface CardCreationService {

    CardCreationResponse createCard(CreateCardRequest request, Long userId);

    CardCreationResponse getCreationStatus(UUID requestId, Long userId);

    CardCreationConfigResponse getCreationConfig(Long userId);

    void reconcilePendingOrders();

    void reconcile(String requestId);
}
