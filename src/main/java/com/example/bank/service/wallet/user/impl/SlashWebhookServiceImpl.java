package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.config.slash.SlashClient;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.wallet.user.SlashTransactionResponse;
import com.example.bank.entity.wallet.Card;
import com.example.bank.entity.wallet.CardTransaction;
import com.example.bank.enums.wallet.CardTransactionStatus;
import com.example.bank.repository.wallet.CardTransactionRepository;
import com.example.bank.service.wallet.user.SlashWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlashWebhookServiceImpl implements SlashWebhookService {

    private final ObjectMapper objectMapper;
    private final SlashClient slashClient;
    private final CardTransactionPersistenceService persistenceService;

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "aggregated_transaction.create",
            "aggregated_transaction.update"
    );

    @Override
    @Async("webhookTaskExecutor")
    public void handleAsync(String payload) {

        try {
            JsonNode node = objectMapper.readTree(payload);
            String event    = node.path("event").asText(null);
            String eventId  = node.path("eventId").asText(null);
            String entityId = node.path("entityId").asText(null);

            log.info("event={} eventId={} entityId={}", event, eventId, entityId);
            // Validate
            if (!SUPPORTED_EVENTS.contains(event)) {
                log.warn("Ignore event={}", event);
                return;
            }

            if (entityId == null || entityId.isBlank()) {
                log.warn("Missing entityId");
                return;
            }

            // Fetch transaction (source of truth)
            SlashTransactionResponse tx = getTransactionWithRetry(entityId);

            if (tx == null || tx.getId() == null) {
                log.error("Cannot fetch transaction entityId={}", entityId);
                return;
            }

            log.info("txId={} status={} detailedStatus={}",
                    tx.getId(), tx.getStatus(), tx.getDetailedStatus());

            // Persist
            persistenceService.upsertTransaction(
                    tx,
                    eventId,
                    payload,
                    mapStatus(tx.getStatus(),tx.getDetailedStatus())
            );
        } catch (Exception e) {
            log.error("WEBHOOK ERROR: {}", e.getMessage(), e);
        }
    }

    // Retry chuẩn
    private SlashTransactionResponse getTransactionWithRetry(String entityId) {
        final int maxRetry = 3;
        final long baseDelayMs = 300;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                log.debug("Fetch attempt {}/{} entityId={}", attempt, maxRetry, entityId);
                SlashTransactionResponse tx = slashClient.getTransaction(entityId);
                if (tx != null && tx.getId() != null) {
                    // nếu FAILED thì return luôn (không cần retry)
                    if ("failed".equalsIgnoreCase(tx.getStatus())) {
                        return tx;
                    }
                    return tx;
                }
            } catch (Exception e) {
                if (isClientError(e)) {
                    log.error("Client error skip retry entityId={} err={}", entityId, e.getMessage());
                    return null;
                }
                log.warn("Retry {}/{} failed entityId={} err={}",
                        attempt, maxRetry, entityId, e.getMessage());
            }
            if (attempt < maxRetry) {
                try {
                    long delay = baseDelayMs * (1L << (attempt - 1)); // 300,600,1200
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        log.error("Exhausted retries entityId={}", entityId);
        return null;
    }

    private boolean isClientError(Exception e) {
        return e.getMessage() != null && e.getMessage().contains("400");
    }

    private CardTransactionStatus mapStatus(String status, String detailedStatus) {
        String s = status != null ? status.trim().toLowerCase() : "";
        String d = detailedStatus != null ? detailedStatus.trim().toLowerCase() : "";

        // Ưu tiên detailedStatus trước
        return switch (d) {
            case "pending" ->
                    CardTransactionStatus.PENDING;

            case "settled" ->
                    CardTransactionStatus.POSTED;

            case "declined", "failed", "canceled" ->
                    CardTransactionStatus.FAILED;

            case "reversed" ->
                    CardTransactionStatus.REVERSED;

            // Hiện tại chưa xử lý refund/returned/dispute
            case "refund", "returned", "dispute" ->
                    CardTransactionStatus.UNKNOWN;

            default -> switch (s) {
                case "pending" ->
                        CardTransactionStatus.PENDING;

                case "settled" ->
                        CardTransactionStatus.POSTED;

                case "declined", "failed", "canceled" ->
                        CardTransactionStatus.FAILED;

                case "reversed" ->
                        CardTransactionStatus.REVERSED;

                case "refund", "returned", "dispute" ->
                        CardTransactionStatus.UNKNOWN;

                default ->
                        CardTransactionStatus.UNKNOWN;
            };
        };
    }

}
