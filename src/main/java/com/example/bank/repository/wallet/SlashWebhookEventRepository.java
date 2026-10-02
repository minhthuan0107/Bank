package com.example.bank.repository.wallet;

import com.example.bank.entity.wallet.SlashWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlashWebhookEventRepository
        extends JpaRepository<SlashWebhookEvent, Long> {

    boolean existsByEventId(String eventId);
}
