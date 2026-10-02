package com.example.bank.entity.wallet;

import com.example.bank.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "slash_webhook_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_slash_webhook_events_event_id",
                        columnNames = "event_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_slash_webhook_events_entity_id",
                        columnList = "entity_id"
                ),
                @Index(
                        name = "idx_slash_webhook_events_processed_at",
                        columnList = "processed_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlashWebhookEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID duy nhất của webhook event do Slash cung cấp
    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    // Loại webhook, ví dụ aggregated_transaction.update
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    // entityId của Slash, với transaction chính là transactionId
    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    // Thời điểm Bank xử lý thành công event
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public static SlashWebhookEvent create(
            String eventId,
            String eventType,
            String entityId
    ) {
        SlashWebhookEvent event = new SlashWebhookEvent();

        event.eventId = eventId;
        event.eventType = eventType;
        event.entityId = entityId;
        event.processedAt = Instant.now();

        return event;
    }
}