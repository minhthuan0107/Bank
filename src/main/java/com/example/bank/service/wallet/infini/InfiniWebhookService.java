package com.example.bank.service.wallet.infini;

public interface InfiniWebhookService {

    /**
     * Verifies the signature and stores the event (deduplicated by event ID), then tries to process it.
     * Throws WalletException (401/503) when the delivery must not be acknowledged.
     */
    void receive(String timestamp, String eventId, String signatureVersion, String signature, byte[] rawBody);

    /** Processes stored events that are not yet processed. Called by the scheduler. */
    void processPending();
}
