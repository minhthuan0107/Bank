package com.example.bank.repository.wallet.slash;

import com.example.bank.entity.wallet.slash.SlashWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlashWebhookEventRepository
        extends JpaRepository<SlashWebhookEvent, Long> {

    boolean existsByEventId(String eventId);
}
