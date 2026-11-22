package com.example.bank.entity.wallet.infini;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** A verified Infini webhook delivery, stored before it is processed. */
@Entity
@Table(name = "infini_webhook_events")
@Getter
@Setter
public class InfiniWebhookEvent {
    public static final String RECEIVED = "RECEIVED";
    public static final String PROCESSED = "PROCESSED";
    public static final String IGNORED = "IGNORED";
    public static final String FAILED = "FAILED";

    @Id
    @Column(length = 100)
    private String eventId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String payload;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(length = 100)
    private String lastError;

    @Column(nullable = false)
    private Instant receivedAt;

    @Column
    private Instant processedAt;
}
