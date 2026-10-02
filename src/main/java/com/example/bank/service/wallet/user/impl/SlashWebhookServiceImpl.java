package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.config.slash.SlashClient;
import com.example.bank.dto.response.wallet.user.SlashTransactionResponse;
import com.example.bank.enums.wallet.CardTransactionStatus;
import com.example.bank.service.wallet.user.SlashWebhookService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlashWebhookServiceImpl implements SlashWebhookService {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "aggregated_transaction.create",
            "aggregated_transaction.update"
    );

    private final ObjectMapper objectMapper;
    private final SlashClient slashClient;
    private final CardTransactionPersistenceService persistenceService;

    @Override
    public void handle(String payload) {
        JsonNode node = parsePayload(payload);

        if (node == null) {
            return;
        }

        String event = node.path("event").asText(null);
        String eventId = node.path("eventId").asText(null);
        String entityId = node.path("entityId").asText(null);

        if (!SUPPORTED_EVENTS.contains(event)) {
            log.debug(
                    "SLASH_WEBHOOK_IGNORED event={} eventId={} entityId={} reason=UNSUPPORTED_EVENT",
                    event,
                    eventId,
                    entityId
            );

            return;
        }

        if (isBlank(eventId)) {
            log.warn(
                    "SLASH_WEBHOOK_INVALID event={} entityId={} reason=MISSING_EVENT_ID",
                    event,
                    entityId
            );

            return;
        }

        if (isBlank(entityId)) {
            log.warn(
                    "SLASH_WEBHOOK_INVALID event={} eventId={} reason=MISSING_ENTITY_ID",
                    event,
                    eventId
            );

            return;
        }

        /*
         * Check nhanh trước khi gọi Slash API.
         *
         * Dùng bảng slash_webhook_events để kiểm tra eventId.
         * PersistenceService sẽ check lại lần nữa bên trong transaction
         * để chống race condition.
         */
        if (persistenceService.isEventProcessed(eventId)) {
            log.debug(
                    "SLASH_WEBHOOK_DUPLICATE event={} eventId={} entityId={}",
                    event,
                    eventId,
                    entityId
            );

            return;
        }

        log.debug(
                "SLASH_WEBHOOK_RECEIVED event={} eventId={} entityId={}",
                event,
                eventId,
                entityId
        );

        /*
         * Webhook chỉ là tín hiệu cho biết transaction đã thay đổi.
         *
         * Không dùng status trong webhook để thay đổi balance.
         * Luôn GET lại transaction hiện tại từ Slash bằng entityId.
         */
        SlashTransactionResponse tx = slashClient.getTransaction(entityId);

        if (tx == null || isBlank(tx.getId())) {
            throw new IllegalStateException(
                    "Slash returned invalid transaction for entityId=" + entityId
            );
        }

        /*
         * entityId của webhook phải đúng với transaction trả về từ Slash.
         * Nếu không đúng thì không được xử lý balance.
         */
        if (!entityId.equals(tx.getId())) {
            throw new IllegalStateException(
                    "Slash transaction id mismatch. entityId="
                            + entityId
                            + ", transactionId="
                            + tx.getId()
            );
        }

        CardTransactionStatus mappedStatus = mapStatus(
                tx.getStatus(),
                tx.getDetailedStatus()
        );

        /*
         * Các status chưa hỗ trợ như refund / returned / dispute
         * hiện tại không được thay đổi balance.
         */
        if (mappedStatus == CardTransactionStatus.UNKNOWN) {
            log.warn(
                    "SLASH_TRANSACTION_UNKNOWN_STATUS event={} eventId={} txId={} status={} detailedStatus={}",
                    event,
                    eventId,
                    tx.getId(),
                    tx.getStatus(),
                    tx.getDetailedStatus()
            );

            return;
        }

        persistenceService.upsertTransaction(
                tx,
                event,
                eventId,
                payload,
                mappedStatus
        );

        log.debug(
                "SLASH_WEBHOOK_PROCESSED event={} eventId={} txId={} status={} detailedStatus={} mappedStatus={}",
                event,
                eventId,
                tx.getId(),
                tx.getStatus(),
                tx.getDetailedStatus(),
                mappedStatus
        );
    }

    private JsonNode parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn(
                    "SLASH_WEBHOOK_INVALID reason=EMPTY_PAYLOAD"
            );

            return null;
        }

        try {
            return objectMapper.readTree(payload);

        } catch (JsonProcessingException e) {
            log.warn(
                    "SLASH_WEBHOOK_INVALID reason=INVALID_JSON message={}",
                    e.getOriginalMessage()
            );

            return null;
        }
    }

    private CardTransactionStatus mapStatus(
            String status,
            String detailedStatus
    ) {
        String baseStatus = normalize(status);
        String detailStatus = normalize(detailedStatus);

        /*
         * detailedStatus được ưu tiên vì mô tả trạng thái
         * nghiệp vụ cụ thể hơn base status.
         */
        return switch (detailStatus) {
            case "pending" ->
                    CardTransactionStatus.PENDING;

            case "settled" ->
                    CardTransactionStatus.POSTED;

            case "declined",
                 "failed",
                 "canceled",
                 "cancelled" ->
                    CardTransactionStatus.FAILED;

            /*
             * Reversal của transaction.
             * Transaction không còn giữ tiền trong local balance.
             */
            case "reversed" ->
                    CardTransactionStatus.REVERSED;

            /*
             * Chưa hỗ trợ các nghiệp vụ này.
             *
             * Không fallback xuống base status vì ví dụ:
             * baseStatus = posted
             * detailedStatus = refund
             *
             * Nếu fallback thành POSTED có thể làm sai balance.
             */
            case "refund",
                 "returned",
                 "dispute" ->
                    CardTransactionStatus.UNKNOWN;

            default ->
                    mapBaseStatus(baseStatus);
        };
    }

    private CardTransactionStatus mapBaseStatus(String status) {
        return switch (status) {
            case "pending" ->
                    CardTransactionStatus.PENDING;

            case "posted",
                 "settled" ->
                    CardTransactionStatus.POSTED;

            case "failed",
                 "declined",
                 "canceled",
                 "cancelled" ->
                    CardTransactionStatus.FAILED;

            case "reversed" ->
                    CardTransactionStatus.REVERSED;

            default ->
                    CardTransactionStatus.UNKNOWN;
        };
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}