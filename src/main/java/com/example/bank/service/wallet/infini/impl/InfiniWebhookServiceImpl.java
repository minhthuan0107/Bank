package com.example.bank.service.wallet.infini.impl;

import com.example.bank.common.config.infini.InfiniProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import com.example.bank.entity.wallet.infini.InfiniWebhookEvent;
import com.example.bank.repository.wallet.infini.CardCreationOrderRepository;
import com.example.bank.repository.wallet.infini.InfiniFundingOrderRepository;
import com.example.bank.repository.wallet.infini.InfiniWebhookEventRepository;
import com.example.bank.service.wallet.infini.CardCreationService;
import com.example.bank.service.wallet.infini.CardCreationTransactionService;
import com.example.bank.service.wallet.infini.InfiniFundingTransactionService;
import com.example.bank.service.wallet.infini.InfiniWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import static com.example.bank.entity.wallet.infini.InfiniWebhookEvent.*;

/**
 * Infini webhooks (signature: hex HMAC-SHA256 of "{timestamp}.{event_id}.{raw body}").
 * Events are only signals: money moves through the same ledger methods as the API flows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InfiniWebhookServiceImpl implements InfiniWebhookService {

    // The docs give no window; retries span about 16 minutes. Replays are also stopped by the event ID.
    private static final Duration TIMESTAMP_TOLERANCE = Duration.ofHours(1);
    private static final int MAX_ATTEMPTS = 10;

    private final InfiniProperties properties;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;
    private final InfiniWebhookEventRepository events;
    private final InfiniFundingOrderRepository fundingOrders;
    private final InfiniFundingTransactionService fundingLedger;
    private final CardCreationOrderRepository creationOrders;
    private final CardCreationTransactionService creationLedger;
    private final CardCreationService creationService;

    @Override
    public void receive(String timestamp, String eventId, String signatureVersion, String signature, byte[] rawBody) {
        String secret = properties.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            // Not acknowledged, so Infini retries once the secret is configured.
            throw new WalletException(MessageKeys.INFINI_WEBHOOK_NOT_CONFIGURED, HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (!verify(secret, timestamp, eventId, signature, rawBody)) {
            throw new WalletException(MessageKeys.INFINI_WEBHOOK_INVALID_SIGNATURE, HttpStatus.UNAUTHORIZED);
        }
        if (store(eventId, rawBody)) {
            process(eventId);
        }
    }

    boolean verify(String secret, String timestamp, String eventId, String signature, byte[] rawBody) {
        if (timestamp == null || eventId == null || signature == null || rawBody == null ||
                eventId.isBlank() || eventId.length() > 100) {
            return false;
        }
        try {
            Instant sentAt = Instant.ofEpochSecond(Long.parseLong(timestamp.trim()));
            if (Duration.between(sentAt, Instant.now()).abs().compareTo(TIMESTAMP_TOLERANCE) > 0) {
                return false;
            }
            ByteArrayOutputStream signed = new ByteArrayOutputStream();
            signed.writeBytes((timestamp + "." + eventId + ".").getBytes(StandardCharsets.UTF_8));
            signed.writeBytes(rawBody);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(signed.toByteArray()));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                    signature.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return false;
        }
    }

    /** Stores the event once. Returns false for a duplicate delivery. */
    private boolean store(String eventId, byte[] rawBody) {
        String payload = new String(rawBody, StandardCharsets.UTF_8);
        String type;
        try {
            type = mapper.readTree(payload).path("event").asText("unknown");
        } catch (Exception e) {
            type = "unparseable";
        }
        String eventType = type.length() > 50 ? type.substring(0, 50) : type;
        try {
            Boolean created = tx.execute(status -> {
                if (events.existsById(eventId)) return false;
                InfiniWebhookEvent event = new InfiniWebhookEvent();
                event.setEventId(eventId);
                event.setEventType(eventType);
                event.setPayload(payload);
                event.setStatus(RECEIVED);
                event.setReceivedAt(Instant.now());
                events.saveAndFlush(event);
                return true;
            });
            return Boolean.TRUE.equals(created);
        } catch (DataIntegrityViolationException e) {
            return false; // Concurrent delivery of the same event.
        }
    }

    @Override
    public void processPending() {
        // Skip very recent events: the receiving request is still processing them.
        for (String id : events.findPending(Instant.now().minusSeconds(30), MAX_ATTEMPTS, PageRequest.of(0, 25))) {
            process(id);
        }
    }

    void process(String eventId) {
        InfiniWebhookEvent event = events.findById(eventId).orElse(null);
        if (event == null || !RECEIVED.equals(event.getStatus())) return;
        try {
            String outcome = handle(mapper.readTree(event.getPayload()));
            finish(eventId, outcome, null);
        } catch (Exception e) {
            log.warn("Infini webhook processing failed eventId={} reason={}", eventId, e.getClass().getSimpleName());
            finish(eventId, null, e.getClass().getSimpleName());
        }
    }

    /** Returns PROCESSED or IGNORED. */
    private String handle(JsonNode root) {
        String event = root.path("event").asText();
        JsonNode data = root.path("data");
        return switch (event) {
            case "card.transaction" -> handleTransaction(data);
            case "card.status_change" -> handleStatusChange(data);
            default -> IGNORED;
        };
    }

    private String handleTransaction(JsonNode data) {
        String providerType = data.path("type").asText();
        String type = switch (providerType) {
            case "top_up" -> InfiniFundingOrder.TOP_UP;
            case "redeem" -> InfiniFundingOrder.REDEEM;
            default -> null; // Spending, refunds etc. are not handled yet.
        };
        if (type == null) return IGNORED;
        String status = data.path("status").asText();
        String txId = data.path("transaction_id").asText("");
        String providerCardId = cardId(data);
        if (txId.isBlank() || providerCardId.isBlank()) return IGNORED;

        var known = fundingOrders.findByProviderTxId(txId);
        if (known.isPresent()) {
            if ("failed".equals(status)) fundingLedger.flagProviderFailure(known.get().getRequestId());
            return PROCESSED;
        }
        if (!"completed".equals(status) && !"failed".equals(status)) return IGNORED; // Not final yet.

        // At most one unresolved order per card exists, so the match is unambiguous.
        List<InfiniFundingOrder> candidates = fundingOrders.findUnresolvedSent(providerCardId, type);
        BigDecimal amount = data.hasNonNull("amount") ? new BigDecimal(data.get("amount").asText()).abs() : null;
        if (candidates.size() != 1 || amount == null || amount.compareTo(candidates.get(0).getAmount()) != 0) {
            // E.g. a top-up made outside this app: never attach it to an order.
            log.warn("Unmatched Infini funding event type={} candidates={}", type, candidates.size());
            return IGNORED;
        }
        fundingLedger.resolveFromProvider(candidates.get(0).getRequestId(), txId, "completed".equals(status));
        return PROCESSED;
    }

    // Speeds up card creation: the alias is the creation request ID.
    private String handleStatusChange(JsonNode data) {
        String alias = data.path("card").path("alias").asText("");
        String providerCardId = cardId(data);
        if (alias.isBlank()) return IGNORED;
        var order = creationOrders.findById(alias).orElse(null);
        if (order == null) return IGNORED;
        if (order.getProviderCardId() == null && !providerCardId.isBlank()) {
            creationLedger.providerLookup(alias, List.of(providerCardId));
        }
        // Settlement still checks the card through the status API.
        creationService.reconcile(alias);
        return PROCESSED;
    }

    private String cardId(JsonNode data) {
        String id = data.path("card").path("card_id").asText("");
        return id.isBlank() ? data.path("card_id").asText("") : id;
    }

    private void finish(String eventId, String outcome, String error) {
        tx.executeWithoutResult(status -> events.findById(eventId).ifPresent(event -> {
            if (outcome != null) {
                event.setStatus(outcome);
                event.setProcessedAt(Instant.now());
                event.setLastError(null);
            } else {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(error);
                if (event.getAttempts() >= MAX_ATTEMPTS) event.setStatus(InfiniWebhookEvent.FAILED);
            }
        }));
    }
}
