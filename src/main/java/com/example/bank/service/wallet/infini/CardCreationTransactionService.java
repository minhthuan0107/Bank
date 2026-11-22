package com.example.bank.service.wallet.infini;

import com.example.bank.dto.request.wallet.infini.CreateCardRequest;
import com.example.bank.entity.wallet.infini.CardCreationOrder;
import com.fasterxml.jackson.databind.JsonNode;

public interface CardCreationTransactionService {

    record Reservation(CardCreationOrder order, boolean submit) {}

    Reservation reserve(CreateCardRequest request, Long userId, String tokenType, String applicantEmail, Integer productId);

    /**
     * Marks that the apply request is about to be sent.
     * Returns false if the order is no longer pending, in which case the request must not be sent.
     */
    boolean markSubmitting(String requestId);

    void accepted(String requestId, JsonNode result);

    void review(String requestId, String reason);

    /** The provider definitely did not create a card: release the held funds. */
    void rejected(String requestId, String reason);

    /** Releases funds of an order whose apply request was never sent. */
    void expireUnsent(String requestId);

    /**
     * Result of looking the card up at the provider by its alias (the request ID) for an order
     * whose apply request was sent but never produced a stored provider card ID.
     */
    void providerLookup(String requestId, java.util.List<String> providerCardIds);

    /** A status or lookup call failed temporarily; keep the order and try again later. */
    void statusUnavailable(String requestId);

    void synchronize(String requestId, JsonNode snapshot);
}
