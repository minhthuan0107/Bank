package com.example.bank.service.wallet.infini;

import com.example.bank.dto.request.wallet.infini.InfiniFundingRequest;
import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

/**
 * Money movements for Infini card top-ups and withdrawals. Each method runs in its own short
 * transaction, so no lock is held while calling Infini.
 */
public interface InfiniFundingTransactionService {

    record Reservation(InfiniFundingOrder order, boolean submit) {}

    /**
     * Validates and stores the order. A top-up holds the amount in the wallet (available -> frozen).
     * For a withdrawal, providerCardBalance is the card balance just read from Infini.
     * A repeated request ID with the same content returns the stored order with submit = false.
     */
    Reservation reserve(String type, InfiniFundingRequest request, Long userId, Long cardId,
                        String tokenType, BigDecimal providerCardBalance);

    /** Returns false if the order may no longer be sent. */
    boolean markSubmitting(String requestId);

    /** Infini accepted the request (tx_id present): settle exactly once. */
    void succeeded(String requestId, JsonNode result);

    /** Infini definitely did not move funds: release a top-up hold, fail the order. */
    void rejected(String requestId, String reason);

    /** Result unknown: keep any hold and wait for a webhook or an operator. */
    void review(String requestId, String reason);

    /** Releases an order whose provider request was never sent. */
    void expireUnsent(String requestId);

    /** An order that was sent but whose result was never recorded. */
    void reviewInterrupted(String requestId);

    /** Infini reported the final result of a sent order whose result was unknown. */
    void resolveFromProvider(String requestId, String providerTxId, boolean completed);

    /** Infini reported a failure for an order already settled as succeeded. Flags it, moves no money. */
    void flagProviderFailure(String requestId);
}
