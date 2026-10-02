CREATE TABLE slash_webhook_events (
                                      id BIGINT NOT NULL AUTO_INCREMENT,

                                      event_id VARCHAR(100) NOT NULL,
                                      event_type VARCHAR(100) NOT NULL,
                                      entity_id VARCHAR(100) NOT NULL,

                                      processed_at DATETIME(6) NOT NULL,

                                      created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                      updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                      PRIMARY KEY (id),

                                      CONSTRAINT uk_slash_webhook_events_event_id
                                          UNIQUE (event_id),

                                      INDEX idx_slash_webhook_events_entity_id (entity_id),
                                      INDEX idx_slash_webhook_events_processed_at (processed_at)
);