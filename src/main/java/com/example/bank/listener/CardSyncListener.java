package com.example.bank.listener;

import com.example.bank.common.config.slash.SlashClient;
import com.example.bank.dto.response.slash.SlashCardDetailResponse;
import com.example.bank.event.CardCreatedEvent;
import com.example.bank.repository.wallet.CardRepository;
import com.example.bank.service.sync.CardSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardSyncListener {

    private final CardSyncService cardSyncService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CardCreatedEvent event) {
        try {
            Thread.sleep(3000);
            cardSyncService.syncCard(event.slashCardId(), event.cardId());
        } catch (Exception e) {
            log.error("SYNC FAIL {}", e.getMessage());
        }
    }
}
